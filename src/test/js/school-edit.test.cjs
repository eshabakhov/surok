const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const { test } = require('node:test');

/** Creates an edit page with the shared CSRF setup and a stubbed AJAX transport. */
const page = ({ kind = 'school', failure } = {}) => {
    const requests = [];
    const visits = [];
    const names = {
        school: 'Лицей № 42', teacher: 'Анна Петрова',
        schedule: 'Расписание на весну', building: 'Северный корпус'
    };
    const input = { value: names[kind] };
    const grade = { valueAsNumber: 7 };
    const litera = { value: 'Б' };
    const button = { disabled: true };
    const message = { hidden: true, textContent: '' };
    let listener;
    let protect;
    const segments = {
        school: '', teacher: '/teachers/31', schedule: '/schedules/31',
        'school-class': '/classes/31', building: '/buildings/31'
    };
    const fields = new Map([
        ['[name="name"]', input], ['[name="grade"]', grade], ['[name="litera"]', litera],
        ['button[type="submit"]', button]
    ]);
    const form = {
        action: 'https://school.test/schools/12' + segments[kind],
        querySelector: selector => fields.get(selector),
        addEventListener: (type, handler) => { if (type === 'submit') { listener = handler; } }
    };
    const elements = new Map([
        [`#${kind}-edit-form`, form],
        [`#${kind}-form-error`, message],
        ['meta[name="_csrf"]', { content: 'masked-token' }],
        ['meta[name="_csrf_header"]', { content: 'X-XSRF-TOKEN' }]
    ]);
    const $ = {
        ajaxSetup: settings => { protect = settings.beforeSend; },
        ajax: request => {
            const headers = { ...request.headers };
            protect({ setRequestHeader: (name, value) => { headers[name] = value; } }, request);
            requests.push({ ...request, headers });
            return {
                done(callback) {
                    if (!failure) { callback(); }
                    return this;
                },
                fail(callback) {
                    if (failure) { callback(failure); }
                }
            };
        }
    };
    const window = { jQuery: $, location: { assign: url => visits.push(url) } };
    const document = { querySelector: selector => elements.get(selector) };
    const script = file => fs.readFileSync('src/main/resources/static/js/' + file, 'utf8')
        .replace(/^import .*;\r?\n/gm, '')
        .replace(/^export const /gm, 'const ');
    vm.runInNewContext(
        ['security.js', 'shared/update.js', 'shared/rename.js', `school/${kind}-edit.js`].map(script).join('\n'),
        { $, document, window }
    );
    return {
        requests, visits, input, grade, litera, button, message,
        submit: () => {
            let intercepted = false;
            listener({ preventDefault: () => { intercepted = true; } });
            return intercepted;
        }
    };
};

test('The edit form saves through JSON PUT with CSRF and opens the school', () => {
    const school = page();
    const intercepted = school.submit();
    const request = school.requests[0];
    assert.deepEqual(
        [intercepted, request.method, request.url, request.contentType, request.headers.Accept,
            request.headers['X-XSRF-TOKEN'], JSON.parse(request.data), school.visits],
        [true, 'PUT', 'https://school.test/schools/12', 'application/json',
            'application/vnd.surok.school-v1+json', 'masked-token',
            { name: 'Лицей № 42' }, ['https://school.test/schools/12']],
        'The edit form did not use the protected canonical update'
    );
});

test('A validation error leaves the edited name available for another attempt', () => {
    const school = page({ failure: { status: 400 } });
    school.input.value = '  ';
    school.submit();
    assert.deepEqual(
        [school.input.value, school.button.disabled, school.message.hidden,
            school.message.textContent, school.visits],
        ['  ', false, false, 'Название не может быть пустым', []],
        'The edit form lost the input or failed to show the validation error'
    );
});

test('The teacher form sends versioned JSON PUT with CSRF and opens the teacher', () => {
    const teacher = page({ kind: 'teacher' });
    const intercepted = teacher.submit();
    const request = teacher.requests[0];
    const uri = 'https://school.test/schools/12/teachers/31';
    const media = 'application/vnd.surok.teacher-v1+json';
    assert.deepEqual(
        [intercepted, request.method, request.url, request.contentType, request.headers.Accept,
            request.headers['X-XSRF-TOKEN'], JSON.parse(request.data), teacher.visits],
        [true, 'PUT', uri, media, media, 'masked-token', { name: 'Анна Петрова' }, [uri]],
        'The teacher edit form did not use the protected canonical update'
    );
});

test('A duplicate teacher name is reported without clearing the input', () => {
    const teacher = page({ kind: 'teacher', failure: { status: 409 } });
    teacher.submit();
    assert.deepEqual(
        [teacher.input.value, teacher.button.disabled, teacher.message.hidden,
            teacher.message.textContent, teacher.visits],
        ['Анна Петрова', false, false, 'Учитель с таким именем уже существует', []],
        'The teacher edit form lost the input or failed to show the conflict'
    );
});

test('The schedule form sends versioned JSON PUT with CSRF and opens the schedule', () => {
    const schedule = page({ kind: 'schedule' });
    const intercepted = schedule.submit();
    const request = schedule.requests[0];
    const uri = 'https://school.test/schools/12/schedules/31';
    const media = 'application/vnd.surok.schedule-v1+json';
    assert.deepEqual(
        [intercepted, request.method, request.url, request.contentType, request.headers.Accept,
            request.headers['X-XSRF-TOKEN'], JSON.parse(request.data), schedule.visits],
        [true, 'PUT', uri, media, media, 'masked-token', { name: 'Расписание на весну' }, [uri]],
        'The schedule edit form did not use the protected canonical update'
    );
});

test('A duplicate schedule name is reported without clearing the input', () => {
    const schedule = page({ kind: 'schedule', failure: { status: 409 } });
    schedule.submit();
    assert.deepEqual(
        [schedule.input.value, schedule.button.disabled, schedule.message.hidden,
            schedule.message.textContent, schedule.visits],
        ['Расписание на весну', false, false, 'Расписание с таким названием уже существует', []],
        'The schedule edit form lost the input or failed to show the conflict'
    );
});

test('The class edit form sends both fields with CSRF and opens the class', () => {
    const clazz = page({ kind: 'school-class' });
    const intercepted = clazz.submit();
    const request = clazz.requests[0];
    const uri = 'https://school.test/schools/12/classes/31';
    const media = 'application/vnd.surok.school-class-v1+json';
    assert.deepEqual(
        [intercepted, request.method, request.url, request.contentType, request.headers.Accept,
            request.headers['X-XSRF-TOKEN'], JSON.parse(request.data), clazz.visits],
        [true, 'PUT', uri, media, media, 'masked-token', { grade: 7, litera: 'Б' }, [uri]],
        'The class edit form did not submit both fields through the protected update'
    );
});

test('A duplicate class is reported without clearing the grade or letter', () => {
    const clazz = page({ kind: 'school-class', failure: { status: 409 } });
    clazz.submit();
    assert.deepEqual(
        [clazz.grade.valueAsNumber, clazz.litera.value, clazz.button.disabled, clazz.message.hidden,
            clazz.message.textContent, clazz.visits],
        [7, 'Б', false, false, 'Класс с такой параллелью и литерой уже существует', []],
        'The class edit form lost the fields or failed to show the conflict'
    );
});

test('The building edit form sends protected JSON PUT and opens the building', () => {
    const building = page({ kind: 'building' });
    const intercepted = building.submit();
    const request = building.requests[0];
    const uri = 'https://school.test/schools/12/buildings/31';
    const media = 'application/vnd.surok.building-v1+json';
    assert.deepEqual(
        [intercepted, request.method, request.url, request.contentType, request.headers.Accept,
            request.headers['X-XSRF-TOKEN'], JSON.parse(request.data), building.visits],
        [true, 'PUT', uri, media, media, 'masked-token', { name: 'Северный корпус' }, [uri]],
        'The building edit form did not use the protected canonical update'
    );
});

test('A building validation error keeps the entered name and allows another attempt', () => {
    const building = page({ kind: 'building', failure: { status: 400 } });
    building.input.value = '  ';
    building.submit();
    assert.deepEqual(
        [building.input.value, building.button.disabled, building.message.hidden,
            building.message.textContent, building.visits],
        ['  ', false, false, 'Название не может быть пустым', []],
        'The building edit form lost the input or failed to show the validation error'
    );
});

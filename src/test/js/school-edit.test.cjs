const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const { test } = require('node:test');

/** Creates an edit page with the shared CSRF setup and a stubbed AJAX transport. */
const page = failure => {
    const requests = [];
    const visits = [];
    const input = { value: 'Лицей № 42' };
    const button = { disabled: true };
    const message = { hidden: true, textContent: '' };
    let listener;
    let protect;
    const form = {
        action: 'https://school.test/schools/12',
        querySelector: selector => selector === '#school-name' ? input : button,
        addEventListener: (type, handler) => { if (type === 'submit') { listener = handler; } }
    };
    const elements = new Map([
        ['#school-edit-form', form],
        ['#school-form-error', message],
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
    for (const script of ['security.js', 'school/school-edit.js']) {
        vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/' + script, 'utf8'), {
            $, document, window
        });
    }
    return {
        requests, visits, input, button, message,
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
    const school = page({ status: 400 });
    school.input.value = '  ';
    school.submit();
    assert.deepEqual(
        [school.input.value, school.button.disabled, school.message.hidden,
            school.message.textContent, school.visits],
        ['  ', false, false, 'Название не может быть пустым', []],
        'The edit form lost the input or failed to show the validation error'
    );
});

const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const { test } = require('node:test');

const script = file => fs.readFileSync(
    `src/main/resources/static/js/${file}`, 'utf8'
).replace(/^import .*;\r?\n/gm, '').replace(/^export const /gm, 'const ');

test('The curriculum edit form submits versioned JSON through protected PUT', () => {
    const fields = {
        title: 'Основной план', level: 'BASIC', week: 'FIVE_DAYS',
        version: '2', year: '2026/2027', description: 'Описание'
    };
    const button = { disabled: true };
    const message = { hidden: true };
    const requests = [];
    const visits = [];
    let listener;
    let protect;
    const form = {
        action: '/federal/curriculums/17',
        querySelector: () => button,
        addEventListener: (_, handler) => { listener = handler; }
    };
    const document = { querySelector: selector => ({
        '#curriculum-edit-form': form,
        '#curriculum-form-error': message,
        'meta[name="_csrf"]': { content: 'masked-token' },
        'meta[name="_csrf_header"]': { content: 'X-XSRF-TOKEN' }
    })[selector] };
    const $ = {
        ajaxSetup: settings => { protect = settings.beforeSend; },
        ajax: request => {
            const headers = { ...request.headers };
            protect({ setRequestHeader: (name, value) => { headers[name] = value; } }, request);
            requests.push({ ...request, headers });
            return { done(callback) { callback(); return this; }, fail() {} };
        }
    };
    const FormData = class {
        entries() { return Object.entries(fields); }
    };
    const window = { jQuery: $, FormData, location: { assign: uri => visits.push(uri) } };
    vm.runInNewContext(
        ['security.js', 'shared/update.js', 'federal/curriculum-edit.js']
            .map(script).join('\n'),
        { $, document, window }
    );
    listener({ preventDefault() {} });
    assert.deepEqual(
        [requests[0].method, requests[0].url, requests[0].contentType,
            requests[0].headers.Accept, requests[0].headers['X-XSRF-TOKEN'],
            JSON.parse(requests[0].data), visits],
        ['PUT', '/federal/curriculums/17',
            'application/vnd.surok.federal-curriculum-v2+json',
            'application/vnd.surok.federal-curriculum-v2+json', 'masked-token',
            fields, ['/federal/curriculums/17']]
    );
});

test('An inline requirement edit sends the selected fields to the item URL', () => {
    const requests = [];
    const error = { text: '', hidden: true, text(value) { this.text = value; return this; },
        prop(name, value) { this[name] = value; return this; } };
    const button = { prop() { return this; } };
    const values = {
        '.requirement-grade-input': '5', '.requirement-subject-input': '31',
        '.requirement-hours-input': '7'
    };
    const row = {
        find: selector => values[selector] === undefined
            ? selector === '.requirement-edit-error' ? error : button
            : { val: () => values[selector] },
        attr: () => 'MANDATORY',
        data: () => '/federal/curriculums/17/requirements/9'
    };
    const $ = {
        ajax: request => {
            requests.push(request);
            return { done() { return this; }, fail() {} };
        }
    };
    vm.runInNewContext(
        `${script('federal/requirement.js')}\nrequirement(row).save();`,
        { $, row, window: { location: { reload() {} } } }
    );
    assert.deepEqual(
        [requests[0].method, requests[0].url, requests[0].contentType,
            requests[0].headers.Accept, JSON.parse(requests[0].data)],
        ['PUT', '/federal/curriculums/17/requirements/9',
            'application/vnd.surok.federal-curriculum-requirement-v1+json',
            'application/vnd.surok.federal-curriculum-requirement-v1+json',
            { grade: '5', hours: '7', partType: 'MANDATORY', subjectId: '31' }]
    );
});

const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const { test } = require('node:test');

test('School sorting requests and replaces the results fragment by media type', () => {
    const script = file => fs.readFileSync('src/main/resources/static/js/' + file, 'utf8')
        .replace(/^import .*;\r?\n/gm, '')
        .replace(/^export const /gm, 'const ');
    const document = {};
    const handlers = new Map();
    const requests = [];
    const visits = [];
    const replacements = [];
    const html = '<div id="schools-results">Лицей</div>';
    const window = {
        location: { pathname: '/schools', search: '?filter=name%3A%D0%9B%D0%B8%D1%86%D0%B5%D0%B9&limit=5' },
        addEventListener: () => {}
    };
    const $ = selector => {
        if (typeof selector === 'function') {
            return selector();
        }
        if (selector === document) {
            return { on: (event, target, callback) => handlers.set(target, callback) };
        }
        if (selector === '#schools-results') {
            return { replaceWith: value => replacements.push(value) };
        }
        return { val: () => 'Лицей', data: name => selector[name] };
    };
    $.get = request => {
        requests.push(typeof request === 'string' ? { url: request } : request);
        return { done: callback => callback(html) };
    };
    vm.runInNewContext(
        ['shared/query.js', 'shared/delayed.js', 'shared/filters.js', 'shared/sorts.js',
            'shared/fragment.js', 'school/schools.js'].map(script).join('\n'),
        { $, document, window, URLSearchParams, history: { pushState: (state, title, url) => visits.push(url) } }
    );
    handlers.get('.schools-sort').call({ sort: 'name' });
    const request = new URL(requests[0].url, 'https://school.test');
    const page = new URL(visits[0], 'https://school.test');
    assert.deepEqual(
        [request.pathname, request.searchParams.has('fragment'), request.searchParams.get('filter'),
            request.searchParams.get('limit'), page.href, requests[0].headers?.Accept, replacements[0]],
        ['/schools', false, 'name:Лицей', '5', request.href, 'text/vnd.surok.schools-fragment+html', html],
        'The school results were not refreshed by media type on the canonical URL'
    );
});

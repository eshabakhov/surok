const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const { test } = require('node:test');

for (const resource of ['teacher', 'schedule']) {
    test(`${resource} sorting requests and replaces the fragment at the collection URL`, () => {
        const script = file => fs.readFileSync('src/main/resources/static/js/' + file, 'utf8')
            .replace(/^import .*;\r?\n/gm, '')
            .replace(/^export const /gm, 'const ');
        const document = {};
        const handlers = new Map();
        const requests = [];
        const visits = [];
        const replacements = [];
        const path = `/schools/12/${resource}s`;
        const html = `<div id="${resource}s-results">Анна Петрова</div>`;
        const window = {
            location: { pathname: path, search: '?filter=name%3A%D0%90%D0%BD%D0%BD%D0%B0&limit=5' },
            addEventListener: () => {}
        };
        let search = '';
        const $ = selector => {
            if (typeof selector === 'function') {
                return selector();
            }
            if (selector === document) {
                return { on: (event, target, callback) => handlers.set(target, callback) };
            }
            if (selector === `#${resource}s-results`) {
                return { replaceWith: value => replacements.push(value) };
            }
            if (selector === `#${resource}-search`) {
                return {
                    length: 1,
                    val(value) {
                        if (value !== undefined) { search = value; }
                        return search;
                    }
                };
            }
            return { data: name => selector[name] };
        };
        $.get = request => {
            requests.push(request);
            return { done: callback => callback(html) };
        };
        vm.runInNewContext(
            ['shared/query.js', 'shared/delayed.js', 'shared/filters.js', 'shared/sorts.js',
                'shared/fragment.js', 'shared/collection.js', 'shared/removal.js',
                `school/${resource}s.js`].map(script).join('\n'),
            { $, document, window, URLSearchParams, history: { pushState: (state, title, url) => visits.push(url) } }
        );
        handlers.get(`.${resource}s-sort`).call({ sort: 'name' });
        const request = new URL(requests[0].url, 'https://school.test');
        assert.deepEqual(
            [request.pathname, request.searchParams.get('filter'), request.searchParams.get('limit'),
                requests[0].url, requests[0].headers.Accept, replacements[0]],
            [path, 'name:Анна', '5', visits[0], `text/vnd.surok.${resource}s-fragment+html`, html],
            `The ${resource} results were not refreshed by media type on the canonical URL`
        );
    });
}

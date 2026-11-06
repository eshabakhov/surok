/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

/**
 * Load the browser module in an isolated document without starting its page bindings.
 */
async function script() {
    global.document = { querySelector: () => null };
    try {
        return await import(`data:text/javascript;base64,${fs.readFileSync(
            path.resolve(__dirname, '../../main/resources/static/js/permissions.js'),
        ).toString('base64')}`);
    } finally {
        delete global.document;
    }
}

/**
 * Minimal rendered matrix with the same text and selection surfaces as the page.
 */
function matrix(query, resource) {
    const sections = [
        ['Кабинеты', ['Создание кабинета', 'POST /schools/{school}/buildings/{building}/cabinets']],
        ['Корпуса', ['Просмотр корпусов', 'GET /schools/{school}/buildings']],
    ].map(([name, texts]) => {
        const row = {
            hidden: false,
            querySelector: (selector) => ({
                textContent: texts[selector === '.permission-action' ? 0 : 1],
            }),
        };
        return {
            dataset: { resource: name },
            hidden: false,
            rows: [row],
            querySelectorAll: () => [row],
        };
    });
    const fields = {
        '#permission-search': { value: query },
        '#permission-resource': { value: resource },
        '#permission-count': { textContent: '' },
        '#permission-empty': { hidden: true },
    };
    return {
        sections,
        fields,
        querySelector: (selector) => fields[selector],
        querySelectorAll: () => sections,
    };
}

test('finds Russian actions regardless of case and surrounding spaces', async () => {
    const root = matrix('  СОЗДАНИЕ КАБИНЕТА  ', '');
    (await script()).filterPermissions(root);
    assert.deepEqual(
        root.sections.map((section) => section.hidden),
        [false, true],
        'The query failed to isolate the matching resource',
    );
});

test('combines the route query with the selected resource', async () => {
    const root = matrix('/schools/{school}/buildings', 'Корпуса');
    (await script()).filterPermissions(root);
    assert.deepEqual(
        root.sections.map((section) => section.rows[0].hidden),
        [true, false],
        'The endpoint search ignored the selected resource',
    );
});

test('shows the empty result and restores all rows after clearing the search', async () => {
    const root = matrix('несуществующее действие', '');
    const filtering = await script();
    filtering.filterPermissions(root);
    const empty = root.fields['#permission-empty'].hidden;
    root.fields['#permission-search'].value = '';
    filtering.filterPermissions(root);
    assert.deepEqual(
        [empty, root.fields['#permission-empty'].hidden, root.fields['#permission-count'].textContent],
        [false, true, 'Показано действий: 2'],
        'The filter did not recover from an empty result',
    );
});

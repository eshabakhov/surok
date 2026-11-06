/**
 * Filter the server-rendered permission matrix without changing access rules.
 */
export function filterPermissions(root) {
    const query = root.querySelector('#permission-search').value.trim().toLocaleLowerCase('ru');
    const resource = root.querySelector('#permission-resource').value;
    let count = 0;
    for (const section of root.querySelectorAll('.permissions-resource')) {
        let visible = 0;
        for (const row of section.querySelectorAll('.permission-row')) {
            const text = `${row.querySelector('.permission-action').textContent} ${row.querySelector('.permission-endpoint').textContent}`;
            row.hidden = (resource !== '' && section.dataset.resource !== resource)
                || !text.toLocaleLowerCase('ru').includes(query);
            if (!row.hidden) {
                visible += 1;
            }
        }
        section.hidden = visible === 0;
        count += visible;
    }
    root.querySelector('#permission-count').textContent = `Показано действий: ${count}`;
    root.querySelector('#permission-empty').hidden = count !== 0;
}

/**
 * Enable progressive filtering; all rows remain available without JavaScript.
 */
const root = document.querySelector('#permissions');
if (root) {
    root.querySelector('#permission-filters').hidden = false;
    root.querySelector('#permission-search').addEventListener('input', () => filterPermissions(root));
    root.querySelector('#permission-resource').addEventListener('change', () => filterPermissions(root));
    filterPermissions(root);
}

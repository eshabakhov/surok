import { delayed } from './delayed.js';
import { filters } from './filters.js';
import { fragment, remembered } from './fragment.js';
import { query } from './query.js';
import { sorts } from './sorts.js';

const name = source => {
    const selected = filters(source, ['name', 'name_ct']);
    return selected.value('name') || selected.value('name_ct')
        || source.parameter('name', source.parameter('name_ct', ''));
};

const filtered = (source, value) => filters(
    source.replaced('name', []).replaced('name_ct', []),
    ['name', 'name_ct']
).changed('name_ct', '').changed('name', value).query();

export const collection = options => {
    const search = $(options.search);
    if (search.length) {
        const path = window.location.pathname.replace(/\/+$/, '');
        const results = options.accept
            ? fragment(options.results, path, { Accept: options.accept })
            : fragment(options.results, `${path}/fragment`);
        const shown = remembered(results, path);
        search.val(name(query(window.location.search)));

        $(document).on('click', options.rows, function (event) {
            if (!$(event.target).closest('.card-item-actions').length) {
                window.location.href = $(this).find('.card-item-name').attr('href');
            }
        });
        $(document).on(
            'click',
            `${options.pagination} .pagination-btn, ${options.pagination} .pagination-size-btn`,
            function (event) {
                event.preventDefault();
                const current = query(window.location.search);
                shown.show(
                    filtered(current, search.val().trim()).paged(
                        $(this).data('offset') || 1,
                        $(this).data('limit') || current.parameter('limit', '15')
                    )
                );
            }
        );
        const lookup = delayed(300, value => {
            const current = query(window.location.search);
            shown.show(filtered(current, value).paged(1, current.parameter('limit', '15')));
        });
        $(document).on('input', options.search, function () {
            lookup($(this).val().trim());
        });
        $(document).on('click', options.sort, function () {
            const current = query(window.location.search);
            shown.show(
                sorts(filtered(current, search.val().trim()), ['name'])
                    .cycled('name')
                    .query()
                    .paged(1, current.parameter('limit', '15'))
            );
        });
        window.addEventListener('popstate', () => {
            const current = query(window.location.search);
            search.val(name(current));
            results.show(current);
        });
    }
};

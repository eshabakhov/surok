import { delayed } from '../shared/delayed.js';
import { filters } from '../shared/filters.js';
import { fragment, remembered } from '../shared/fragment.js';
import { query } from '../shared/query.js';
import { sorts } from '../shared/sorts.js';

$(() => {
    const path = window.location.pathname.replace(/\/$/, '');
    const fields = ['name'];
    const sortFields = ['name'];
    const results = fragment('#schools-results', path, { Accept: 'text/vnd.surok.schools-fragment+html' });
    const shown = remembered(results, path);
    const search = $('#school-search');
    search.val(filters(query(window.location.search), fields).value('name'));

    $(document).on(
        'click',
        '#schools-container .school-row',
        function (event) {
            if (!$(event.target).closest('.card-item-actions').length) {
                window.location.href = `/schools/${$(this).data('id')}`;
            }
        }
    );
    $(document).on(
        'click',
        '#schools-pagination .pagination-btn, '
            + '#schools-pagination .pagination-size-btn',
        function (event) {
            event.preventDefault();
            const current = query(window.location.search);
            shown.show(
                filters(current, fields)
                    .changed('name', search.val().trim())
                    .query()
                    .paged(
                        $(this).data('offset') || 1,
                        $(this).data('limit') || current.parameter('limit', '15')
                    )
            );
        }
    );
    const lookup = delayed(300, name => {
        const current = query(window.location.search);
        shown.show(
            filters(current, fields)
                .changed('name', name)
                .query()
                .paged(1, current.parameter('limit', '15'))
        );
    });
    $(document).on('input', '#school-search', function () {
        lookup($(this).val().trim());
    });
    $(document).on('click', '.schools-sort', function () {
        const current = query(window.location.search);
        shown.show(
            sorts(current, sortFields)
                .cycled($(this).data('sort'))
                .query()
                .paged(1, current.parameter('limit', '15'))
        );
    });
    window.addEventListener('popstate', () => {
        const current = query(window.location.search);
        search.val(filters(current, fields).value('name'));
        results.show(current);
    });
});

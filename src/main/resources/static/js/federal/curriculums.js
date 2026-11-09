import { delayed } from '../shared/delayed.js';
import { filters } from '../shared/filters.js';
import { fragment, remembered } from '../shared/fragment.js';
import { query } from '../shared/query.js';
import { sorts } from '../shared/sorts.js';

$(() => {
    const path = window.location.pathname;
    const fields = ['title'];
    const sortFields = ['title', 'level', 'week', 'year'];
    const results = fragment('#federal-curriculums-results', path, {
        Accept: 'text/vnd.surok.federal-curriculums-fragment+html'
    });
    const shown = remembered(results, path);
    const search = $('#federal-curriculum-search');
    search.val(filters(query(window.location.search), fields).value('title'));

    $(document).on(
        'click',
        '#federal-curriculums-container .federal-curriculum-row',
        function (event) {
            if (!$(event.target).closest('.card-item-actions').length) {
                window.location.href = `/federal/curriculums/${$(this).data('id')}`;
            }
        }
    );
    $(document).on(
        'click',
        '#federal-curriculums-pagination .pagination-btn, '
            + '#federal-curriculums-pagination .pagination-size-btn',
        function (event) {
            event.preventDefault();
            const current = query(window.location.search);
            shown.show(
                filters(current, fields)
                    .changed('title', search.val().trim())
                    .query()
                    .paged(
                        $(this).data('offset') || 1,
                        $(this).data('limit') || current.parameter('limit', '15')
                    )
            );
        }
    );
    const lookup = delayed(300, title => {
        const current = query(window.location.search);
        shown.show(
            filters(current, fields)
                .changed('title', title)
                .query()
                .paged(1, current.parameter('limit', '15'))
        );
    });
    $(document).on('input', '#federal-curriculum-search', function () {
        lookup($(this).val().trim());
    });
    $(document).on('click', '.federal-curriculums-sort', function () {
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
        search.val(filters(current, fields).value('title'));
        results.show(current);
    });
});

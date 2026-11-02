import { fragment, remembered } from '../shared/fragment.js';
import { query } from '../shared/query.js';

$(() => {
    if ($('#loads-results').length) {
        const base = window.location.pathname.replace(/\/+$/, '');
        const results = fragment('#loads-results', `${base}/fragment`);
        const shown = remembered(results, base);
        $(document).on(
            'click',
            '#loads-pagination .pagination-btn, #loads-pagination .pagination-size-btn',
            function (event) {
                event.preventDefault();
                const current = query(window.location.search);
                shown.show(current.paged(
                    $(this).data('offset') || 1,
                    $(this).data('limit') || current.parameter('limit', '15')
                ));
            }
        );
        window.addEventListener('popstate', () => { results.show(query(window.location.search)); });
    }
});

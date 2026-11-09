import { collection } from '../shared/collection.js';
import { confirmed, removal } from '../shared/removal.js';

$(() => {
    collection({
        search: '#subject-search',
        results: '#subjects-results',
        pagination: '#subjects-pagination',
        rows: '#subjects-container .subject-row',
        sort: '.subjects-sort',
        accept: 'text/vnd.surok.subjects-fragment+html'
    });
    $(document).on('click', '.btn-delete-row', function () {
        const button = $(this);
        confirmed(
            `Удалить «${button.data('name')}»? Это действие нельзя отменить.`,
            removal(
                button.data('uri'),
                () => { window.location.href = button.data('redirect'); },
                () => alert('Не удалось удалить предмет')
            )
        ).execute();
    });
});

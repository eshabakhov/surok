import { collection } from '../shared/collection.js';
import { confirmed, removal } from '../shared/removal.js';

$(() => {
    collection({
        search: '#teacher-search',
        results: '#teachers-results',
        pagination: '#teachers-pagination',
        rows: '#teachers-container .teacher-row',
        sort: '.teachers-sort',
        accept: 'text/vnd.surok.teachers-fragment+html'
    });
    $(document).on('click', '.btn-delete-row', function () {
        const button = $(this);
        confirmed(
            `Удалить «${button.data('name')}»? Это действие нельзя отменить.`,
            removal(
                button.data('uri'),
                () => { window.location.href = button.data('redirect'); },
                () => alert('Не удалось удалить учителя')
            )
        ).execute();
    });
});

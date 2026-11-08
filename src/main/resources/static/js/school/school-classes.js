import { collection } from '../shared/collection.js';
import { confirmed, removal } from '../shared/removal.js';

$(() => {
    collection({
        search: '#school-class-search',
        results: '#school-classes-results',
        pagination: '#school-classes-pagination',
        rows: '#school-classes-container .school-class-row',
        sort: '.school-classes-sort',
        accept: 'text/vnd.surok.school-classes-fragment+html'
    });
    $(document).on('click', '.btn-delete-row', function () {
        const button = $(this);
        confirmed(
            `Удалить «${button.data('name')}»? Это действие нельзя отменить.`,
            removal(
                button.data('uri'),
                () => { window.location.href = button.data('redirect'); },
                () => alert('Не удалось удалить класс')
            )
        ).execute();
    });
});

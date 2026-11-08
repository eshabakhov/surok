import { collection } from '../shared/collection.js';
import { confirmed, removal } from '../shared/removal.js';

$(() => {
    collection({
        search: '#schedule-search',
        results: '#schedules-results',
        pagination: '#schedules-pagination',
        rows: '#schedules-container .schedule-row',
        sort: '.schedules-sort',
        accept: 'text/vnd.surok.schedules-fragment+html'
    });
    $(document).on('click', '.btn-delete-row', function () {
        const button = $(this);
        confirmed(
            `Удалить «${button.data('name')}»? Это действие нельзя отменить.`,
            removal(
                button.data('uri'),
                () => { window.location.href = button.data('redirect'); },
                () => alert('Не удалось удалить расписание')
            )
        ).execute();
    });
});

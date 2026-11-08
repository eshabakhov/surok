import { collection } from '../shared/collection.js';
import { confirmed, removal } from '../shared/removal.js';

$(() => {
    collection({
        search: '#building-search',
        results: '#buildings-results',
        pagination: '#buildings-pagination',
        rows: '#buildings-container .building-row',
        sort: '.buildings-sort',
        accept: 'text/vnd.surok.buildings-fragment+html'
    });
    $(document).on('click', '.btn-delete-row', function () {
        const button = $(this);
        confirmed(
            `Удалить «${button.data('name')}»? Это действие нельзя отменить.`,
            removal(
                button.data('uri'),
                () => { window.location.href = button.data('redirect'); },
                () => alert('Не удалось удалить корпус')
            )
        ).execute();
    });
});

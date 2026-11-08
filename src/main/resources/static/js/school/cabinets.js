import { collection } from '../shared/collection.js';
import { confirmed, removal } from '../shared/removal.js';

$(() => {
    collection({
        search: '#cabinet-search',
        results: '#cabinets-results',
        pagination: '#cabinets-pagination',
        rows: '#cabinets-container .cabinet-row',
        sort: '.cabinets-sort',
        accept: 'text/vnd.surok.cabinets-fragment+html'
    });
    $(document).on('click', '.btn-delete-row', function () {
        const button = $(this);
        confirmed(
            `Удалить «${button.data('name')}»? Это действие нельзя отменить.`,
            removal(
                button.data('uri'),
                () => { window.location.href = button.data('redirect'); },
                () => alert('Не удалось удалить кабинет')
            )
        ).execute();
    });
});

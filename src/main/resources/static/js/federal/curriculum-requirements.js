import { delayed } from '../shared/delayed.js';
import { filters } from '../shared/filters.js';
import { fragment, remembered } from '../shared/fragment.js';
import { query } from '../shared/query.js';
import { confirmed, removal } from '../shared/removal.js';
import { requirement } from './requirement.js';
import { sorts } from '../shared/sorts.js';

$(() => {
    $('#subject-name').on('change', function () {
        const optional = $(this).val() === 'OPTIONAL';
        const label = optional ? 'Максимум часов в неделю' : 'Обязательных часов в неделю';
        $('#requirement-hours').attr({
            min: optional ? '0' : '1',
            placeholder: optional ? 'Максимум ч./нед.' : 'Часов/нед.',
            'aria-label': label
        });
        $('#requirement-hours-hint').text(optional
            ? 'Общий максимум для дополнительных предметов на каждый класс параллели; ноль запрещает вариативную нагрузку.'
            : 'Обязательная нагрузка по выбранному федеральному предмету на каждый класс параллели.');
    }).trigger('change');
    const path = window.location.pathname;
    const filterFields = ['grade', 'subject', 'part'];
    const sortFields = ['grade', 'subject', 'hours', 'part'];
    const results = fragment(
        '#requirements-results',
        $('#requirements-body').data('search-url')
    );
    const shown = remembered(results, path);
    const restored = filters(query(window.location.search), filterFields);
    $('#filter-grade').val(restored.value('grade'));
    $('#filter-subject').val(restored.value('subject'));
    $('#filter-part').val(restored.value('part'));

    const filtered = (name, value) => {
        const current = query(window.location.search);
        shown.show(
            filters(current, filterFields)
                .changed(name, value)
                .query()
                .paged(1, current.parameter('limit', '15'))
        );
    };
    const grade = delayed(250, value => filtered('grade', value));

    $(document).on(
        'click',
        '#requirements-pagination .pagination-btn, '
            + '#requirements-pagination .pagination-size-btn',
        function (event) {
            event.preventDefault();
            const current = query(window.location.search);
            shown.show(
                current.paged(
                    $(this).data('offset') || 1,
                    $(this).data('limit') || current.parameter('limit', '15')
                )
            );
        }
    );
    $(document).on('click', '.requirements-sort', function () {
        const current = query(window.location.search);
        shown.show(
            sorts(current, sortFields)
                .cycled($(this).data('sort'))
                .query()
                .paged(1, current.parameter('limit', '15'))
        );
    });
    $('#filter-grade').on('input', function () {
        grade($(this).val().trim());
    });
    $('#filter-subject').on('change', function () {
        filtered('subject', $(this).val());
    });
    $('#filter-part').on('change', function () {
        filtered('part', $(this).val());
    });
    window.addEventListener('popstate', () => {
        const current = query(window.location.search);
        const currentFilters = filters(current, filterFields);
        $('#filter-grade').val(currentFilters.value('grade'));
        $('#filter-subject').val(currentFilters.value('subject'));
        $('#filter-part').val(currentFilters.value('part'));
        results.show(current);
    });
    $(document).on('click', '.requirement-edit', function () {
        $('#requirements-body tr.requirement-editing').each(function () {
            requirement($(this)).cancel();
        });
        const editable = requirement($(this).closest('tr'));
        editable.cancel();
        editable.edit();
    });
    $(document).on('click', '.requirement-cancel', function () {
        requirement($(this).closest('tr')).cancel();
    });
    $(document).on('click', '.requirement-save', function () {
        requirement($(this).closest('tr')).save();
    });
    $(document).on('click', '.requirement-delete', function () {
        confirmed(
            `Удалить требование «${$(this).data('name')}»?`,
            removal(
                `/federal/curriculums/${$(this).data('curriculum')}`
                    + `/requirements/${$(this).data('id')}`,
                () => window.location.reload(),
                () => alert('Не удалось удалить требование')
            )
        ).execute();
    });
});

export const requirement = row => Object.freeze({
    edit: () => {
        row.find('.requirement-edit-error').prop('hidden', true);
        row.addClass('requirement-editing');
        row.find('.requirement-grade-input').trigger('focus');
    },
    cancel: () => {
        row.find('.requirement-grade-input').val(
            row.find('.requirement-grade .requirement-value').text().trim()
        );
        row.find('.requirement-subject-input').val(
            row.attr('data-subject')
        );
        row.find('.requirement-hours-input').val(
            row.attr('data-hours')
        );
        row.find('.requirement-edit-error').prop('hidden', true);
        row.removeClass('requirement-editing');
    },
    save: () => {
        const grade = String(row.find('.requirement-grade-input').val() || '').trim();
        const subject = String(row.find('.requirement-subject-input').val() || '').trim();
        const hours = row.find('.requirement-hours-input').val().trim();
        const part = row.attr('data-part');
        const error = row.find('.requirement-edit-error');
        if (!grade || (!subject && part === 'MANDATORY') || !hours || !part) {
            error.text('Заполните все поля').prop('hidden', false);
        } else {
            const button = row.find('.requirement-save');
            const fields = { grade, hours, partType: part };
            if (part === 'MANDATORY') {
                fields.subjectId = subject;
            }
            error.prop('hidden', true);
            button.prop('disabled', true);
            $.ajax({
                url: row.data('uri'),
                method: 'PUT',
                contentType: 'application/vnd.surok.federal-curriculum-requirement-v1+json',
                headers: { Accept: 'application/vnd.surok.federal-curriculum-requirement-v1+json' },
                data: JSON.stringify(fields)
            }).done(() => window.location.reload()).fail(failure => {
                error.text(failure.status === 409
                    ? 'Такое требование уже существует.'
                    : 'Не удалось сохранить требование. Проверьте поля и повторите попытку.')
                    .prop('hidden', false);
                button.prop('disabled', false);
            });
        }
    }
});

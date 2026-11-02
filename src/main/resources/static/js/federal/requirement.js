export const requirement = row => Object.freeze({
    edit: () => {
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
        row.removeClass('requirement-editing');
    },
    save: () => {
        const grade = String(row.find('.requirement-grade-input').val() || '').trim();
        const subject = String(row.find('.requirement-subject-input').val() || '').trim();
        const hours = row.find('.requirement-hours-input').val().trim();
        const part = row.attr('data-part');
        if (!grade || (!subject && part === 'MANDATORY') || !hours || !part) {
            alert('Заполните все поля');
        } else {
            const form = row.find('.requirement-edit-form');
            form.find('.requirement-form-grade').val(grade);
            form.find('.requirement-form-subject').val(subject);
            form.find('.requirement-form-hours').val(hours);
            form.find('.requirement-form-part').val(part);
            form.trigger('submit');
        }
    }
});

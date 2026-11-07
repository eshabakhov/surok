const form = document.querySelector('#school-edit-form');
const input = form.querySelector('#school-name');
const button = form.querySelector('button[type="submit"]');
const message = document.querySelector('#school-form-error');
const errors = {
    400: 'Название не может быть пустым',
    401: 'Войдите в систему, чтобы сохранить изменения.',
    403: 'Недостаточно прав или истекла сессия. Обновите страницу и повторите попытку.'
};

/** Submits the JSON update and keeps the form available if it fails. */
form.addEventListener('submit', event => {
    event.preventDefault();
    if (button.disabled) {
        return;
    }
    message.hidden = true;
    button.disabled = true;
    $.ajax({
        url: form.action,
        method: 'PUT',
        contentType: 'application/json',
        headers: { Accept: 'application/vnd.surok.school-v1+json' },
        dataType: 'json',
        data: JSON.stringify({ name: input.value })
    }).done(() => {
        window.location.assign(form.action);
    }).fail(failure => {
        message.textContent = errors[failure.status] ?? 'Не удалось сохранить изменения. Повторите попытку.';
        message.hidden = false;
        button.disabled = false;
    });
});
button.disabled = false;

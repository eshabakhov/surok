/**
 * Connects an edit form to the resource's JSON PUT operation.
 * @param {HTMLFormElement} form Form whose action is the resource URL.
 * @param {HTMLElement} message Inline error element.
 * @param {object} options Payload, media types and validation messages for this resource.
 */
export const update = (form, message, { payload, accept, contentType = accept, errors }) => {
    const button = form.querySelector('button[type="submit"]');
    const notices = {
        401: 'Войдите в систему, чтобы сохранить изменения.',
        403: 'Недостаточно прав или истекла сессия. Обновите страницу и повторите попытку.',
        ...errors
    };
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
            contentType,
            headers: { Accept: accept },
            dataType: 'json',
            data: JSON.stringify(payload())
        }).done(() => {
            window.location.assign(form.action);
        }).fail(failure => {
            message.textContent = notices[failure.status] ?? 'Не удалось сохранить изменения. Повторите попытку.';
            message.hidden = false;
            button.disabled = false;
        });
    });
    button.disabled = false;
};

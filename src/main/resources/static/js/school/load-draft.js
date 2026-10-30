$(() => {
    let busy = false;
    let dirty = false;
    let stale = false;
    const workspace = () => document.querySelector('#load-draft');
    const selected = () => {
        const root = workspace();
        const value = root.querySelector('#draft-curriculum')?.value;
        return [...root.querySelectorAll('[data-plan]')].find(panel => panel.dataset.plan === value);
    };
    const browsing = () => workspace().dataset.saved === 'true'
        && selected()?.dataset.plan !== workspace().dataset.curriculum;
    const notice = message => { workspace().querySelector('#draft-status').textContent = message; };
    const failure = message => {
        const error = workspace().querySelector('#draft-error');
        error.textContent = message;
        error.hidden = false;
    };
    const synchronize = () => {
        const root = workspace();
        const panel = selected();
        const editable = root.dataset.editable === 'true';
        root.setAttribute('aria-busy', String(busy));
        root.querySelectorAll('[data-plan]').forEach(offer => { offer.hidden = offer !== panel; });
        root.querySelectorAll('[data-settings]').forEach(fieldset => {
            const offer = fieldset.closest('[data-plan]');
            fieldset.disabled = busy || stale || !editable || browsing() || (offer !== null && offer !== panel);
        });
        const selector = root.querySelector('#draft-curriculum');
        if (selector) { selector.disabled = busy || stale; }
        const switcher = root.querySelector('#draft-switch');
        if (switcher) {
            root.querySelector('#draft-switch-notice').hidden = !browsing();
            switcher.disabled = busy || stale || !panel;
        }
        const save = root.querySelector('#draft-save');
        if (save) { save.disabled = busy || stale || !panel || browsing(); }
        const apply = root.querySelector('#draft-apply');
        if (apply) {
            apply.disabled = busy || stale || dirty || browsing() || root.dataset.saved !== 'true'
                || !root.querySelector('[data-row]');
        }
        const state = root.querySelector('#draft-state');
        if (state) {
            state.textContent = stale
                ? 'Перезагрузите страницу, чтобы получить сохранённые данные.'
                : dirty ? 'Есть несохранённые изменения. Сохраните их перед применением.'
                : 'Применение использует только сохранённый черновик.';
        }
    };
    const input = () => {
        const additions = [...workspace().querySelectorAll('[data-grade]')].map(grade => ({
            grade: Number(grade.dataset.grade),
            subjects: [...grade.querySelectorAll('[data-addition]')]
                .filter(field => Number(field.value) > 0)
                .map(field => ({
                    subject: { id: Number(field.dataset.subject) }, hours: Number(field.value)
                }))
        }));
        return { curriculum: { id: Number(selected().dataset.plan) }, additions };
    };
    const execute = async (suffix, method, payload, message) => {
        const root = workspace();
        const uri = root.dataset.uri;
        let saved = false;
        busy = true;
        root.querySelector('#draft-error').hidden = true;
        notice('Сохраняем изменения…');
        synchronize();
        try {
            await $.ajax({
                url: `${uri}${suffix}`, method,
                contentType: 'application/vnd.surok.load-draft.v1+json',
                headers: { Accept: 'application/vnd.surok.load-draft.v1+json' },
                dataType: 'json', data: payload === undefined ? undefined : JSON.stringify(payload)
            });
            saved = true;
            dirty = false;
            const html = await $.ajax({
                url: `${uri}/fragment`, method: 'GET', dataType: 'html', cache: false,
                headers: { Accept: 'text/html' }
            });
            const replacement = $($.parseHTML(html, document, false)).filter('#load-draft');
            if (replacement.length !== 1) { throw new Error('Missing planning workspace'); }
            $(root).replaceWith(replacement);
            notice(message);
        } catch (error) {
            notice('');
            if (saved) {
                failure('Изменения сохранены, но страницу не удалось обновить. Перезагрузите её перед продолжением.');
                stale = true;
            } else if (error.status === 401 || error.status === 403) {
                failure('Недостаточно прав или истекла сессия. Войдите в систему и повторите запрос.');
            } else {
                failure(error.responseJSON?.message || 'Не удалось сохранить изменения. Введённые данные сохранены на странице; повторите попытку.');
            }
        } finally {
            busy = false;
            synchronize();
        }
    };
    if (!workspace()) { return; }
    synchronize();
    $(document).on('submit', '#draft-form', event => {
        event.preventDefault();
        if (busy || stale || browsing() || workspace().dataset.editable !== 'true' || !selected()) { return; }
        if (!event.currentTarget.reportValidity()) { return; }
        execute('', 'PUT', input(), 'Черновик сохранён. Обязательные часы взяты из плана, вариативные часы пересчитаны.');
    });
    $(document).on('change', '#draft-curriculum', () => { synchronize(); });
    $(document).on('input change', '#load-draft [data-settings] input, #load-draft [data-settings] select', () => {
        if (busy || stale || workspace().dataset.editable !== 'true') { return; }
        dirty = true;
        synchronize();
    });
    $(document).on('click', '#draft-switch', () => {
        if (busy || stale || !browsing() || workspace().dataset.editable !== 'true') { return; }
        const message = dirty
            ? 'Несохранённые изменения будут потеряны. Сменить план и сбросить дополнительные часы по параллелям?'
            : 'Сменить план и сбросить дополнительные часы по параллелям? Классы и обязательные часы определятся автоматически.';
        if (confirm(message)) {
            execute('/curriculum', 'PUT', { curriculum: { id: Number(selected().dataset.plan) } },
                'Федеральный план изменён. При необходимости задайте дополнительные часы по параллелям.');
        }
    });
    $(document).on('click', '#draft-apply', () => {
        if (busy || stale || dirty || browsing() || workspace().dataset.editable !== 'true'
            || workspace().dataset.saved !== 'true') { return; }
        if (confirm('Применить обязательные часы по плану и сохранённую вариативную нагрузку по параллелям?')) {
            execute('/application', 'POST', undefined, 'Сохранённый черновик применён к учебной нагрузке.');
        }
    });
    window.addEventListener('beforeunload', event => {
        if (dirty || busy) {
            event.preventDefault();
            event.returnValue = '';
        }
    });
});

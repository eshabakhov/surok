import { update } from '../shared/update.js';

const form = document.querySelector('#curriculum-edit-form');
update(form, document.querySelector('#curriculum-form-error'), {
    accept: 'application/vnd.surok.federal-curriculum-v2+json',
    payload: () => Object.fromEntries(
        new window.FormData(form).entries()
    ),
    errors: {
        400: 'Проверьте поля плана и повторите попытку.',
        409: 'План с такими данными уже существует.'
    }
});

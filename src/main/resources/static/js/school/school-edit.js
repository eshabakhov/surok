import { rename } from '../shared/rename.js';

rename(document.querySelector('#school-edit-form'), document.querySelector('#school-form-error'), {
    accept: 'application/vnd.surok.school-v1+json',
    contentType: 'application/json',
    errors: { 400: 'Название не может быть пустым' }
});

import { rename } from '../shared/rename.js';

rename(document.querySelector('#cabinet-edit-form'), document.querySelector('#cabinet-form-error'), {
    accept: 'application/vnd.surok.cabinet-v1+json',
    errors: { 400: 'Название не может быть пустым' }
});

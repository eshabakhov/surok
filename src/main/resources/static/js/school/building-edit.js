import { rename } from '../shared/rename.js';

rename(document.querySelector('#building-edit-form'), document.querySelector('#building-form-error'), {
    accept: 'application/vnd.surok.building-v1+json',
    errors: { 400: 'Название не может быть пустым' }
});

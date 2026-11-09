import { rename } from '../shared/rename.js';

rename(document.querySelector('#subject-edit-form'), document.querySelector('#subject-form-error'), {
    accept: 'application/vnd.surok.subject-v1+json',
    errors: {
        400: 'Название не может быть пустым',
        409: 'Предмет с таким названием уже существует'
    }
});

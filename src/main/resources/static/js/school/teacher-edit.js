import { rename } from '../shared/rename.js';

rename(document.querySelector('#teacher-edit-form'), document.querySelector('#teacher-form-error'), {
    accept: 'application/vnd.surok.teacher-v1+json',
    errors: {
        400: 'Имя не может быть пустым',
        409: 'Учитель с таким именем уже существует'
    }
});

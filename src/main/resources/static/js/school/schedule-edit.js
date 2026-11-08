import { rename } from '../shared/rename.js';

rename(document.querySelector('#schedule-edit-form'), document.querySelector('#schedule-form-error'), {
    accept: 'application/vnd.surok.schedule-v1+json',
    errors: {
        400: 'Название не может быть пустым',
        409: 'Расписание с таким названием уже существует'
    }
});

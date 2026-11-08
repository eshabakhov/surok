import { update } from '../shared/update.js';

const form = document.querySelector('#school-class-edit-form');
const grade = form.querySelector('[name="grade"]');
const litera = form.querySelector('[name="litera"]');
update(form, document.querySelector('#school-class-form-error'), {
    accept: 'application/vnd.surok.school-class-v1+json',
    payload: () => ({ grade: grade.valueAsNumber, litera: litera.value }),
    errors: {
        400: 'Укажите параллель от 1 до 11 и непустую литеру',
        409: 'Класс с такой параллелью и литерой уже существует'
    }
});

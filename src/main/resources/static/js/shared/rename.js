import { update } from './update.js';

/**
 * Connects a name edit form to the resource's JSON PUT operation.
 * @param {HTMLFormElement} form Form whose action is the resource URL.
 * @param {HTMLElement} message Inline error element.
 * @param {object} options Media types and validation messages for this resource.
 */
export const rename = (form, message, options) => {
    const input = form.querySelector('[name="name"]');
    update(form, message, { ...options, payload: () => ({ name: input.value }) });
};

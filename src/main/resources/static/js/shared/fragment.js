/**
 * Refreshes an element with the HTML returned by a GET request.
 * @param {string} target Selector of the element to replace.
 * @param {string} uri Collection or fragment URL.
 * @param {object} headers Optional request headers.
 * @returns {{show: function}} Fragment with a refresh operation.
 */
export const fragment = (target, uri, headers = {}) => Object.freeze({
    show: state => {
        $.get({ url: `${uri}?${state.text()}`, headers }).done(html => {
            $(target).replaceWith(html);
        });
    }
});

export const remembered = (origin, uri) => Object.freeze({
    show: state => {
        history.pushState(null, '', `${uri}?${state.text()}`);
        origin.show(state);
    }
});

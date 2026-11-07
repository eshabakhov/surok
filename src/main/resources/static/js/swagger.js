/** Swagger authentication and documentation use the same verified credentials. */
window.surokSwagger = (() => {
    let authorization = '';
    let token = '';
    let revision = 0;

    /** Removes operations and models while preserving the Authorize dialog. */
    function empty(system) {
        const document = system.specSelectors.specJson().toJS();
        const securitySchemes = document?.components?.securitySchemes ?? {};
        system.specActions.updateSpec(JSON.stringify({
            ...document, paths: {}, components: { securitySchemes }
        }));
    }

    /** Reads an uncached description, rejecting redirects and external origins. */
    async function description(system, credentials) {
        const url = new window.URL(system.specSelectors.url(), window.location.href);
        if (url.origin !== window.location.origin) {
            throw new Error('Описание API должно находиться на сервере приложения');
        }
        const headers = { Accept: 'application/json', 'X-Requested-With': 'XMLHttpRequest' };
        if (credentials) {
            headers.Authorization = credentials;
        }
        const response = await window.fetch(url.href, {
            headers, credentials: 'same-origin', cache: 'no-store', redirect: 'error'
        });
        if (!response.ok) {
            throw new Error(response.status === 401
                ? 'Неверное имя пользователя или пароль'
                : 'Не удалось загрузить доступные операции');
        }
        const document = await response.json();
        if (!document.openapi || !document.paths) {
            throw new Error('Сервер вернул некорректное описание API');
        }
        return { document, token: response.headers.get('X-XSRF-TOKEN') || '' };
    }

    /** Shows a credential error in the existing Authorize dialog. */
    function failure(system, error) {
        system.errActions.newAuthErr({
            authId: 'Basic auth', source: 'auth', level: 'error', message: error.message
        });
    }

    /** Delays Swagger's authorization state until the server validates the password. */
    function plugin() {
        return {
            statePlugins: {
                auth: {
                    wrapActions: {
                        authorize: (original, system) => async credentials => {
                            const attempt = ++revision;
                            authorization = '';
                            empty(system);
                            system.errActions.clear({ source: 'auth' });
                            try {
                                const value = credentials['Basic auth'].value;
                                const encoded = new window.TextEncoder().encode(`${value.username}:${value.password}`);
                                const header = `Basic ${window.btoa(Array.from(encoded, byte => String.fromCharCode(byte)).join(''))}`;
                                const result = await description(system, header);
                                if (attempt === revision) {
                                    authorization = header;
                                    token = result.token;
                                    original(credentials);
                                    system.specActions.updateSpec(JSON.stringify(result.document));
                                }
                            } catch (error) {
                                if (attempt === revision) {
                                    await system.authActions.logout(['Basic auth']);
                                    failure(system, error);
                                }
                            }
                        },
                        logout: (original, system) => async names => {
                            const attempt = ++revision;
                            authorization = '';
                            original(names);
                            empty(system);
                            try {
                                const result = await description(system, '');
                                if (attempt === revision) {
                                    token = result.token;
                                    system.specActions.updateSpec(JSON.stringify(result.document));
                                }
                            } catch (error) {
                                if (attempt === revision) {
                                    failure(system, error);
                                }
                            }
                        }
                    }
                }
            }
        };
    }

    /** Uses verified credentials and a masked CSRF token only on this origin. */
    function request(request) {
        request.headers = request.headers || {};
        for (const name of Object.keys(request.headers)) {
            if (['authorization', 'x-xsrf-token', 'x-surok-swagger'].includes(name.toLowerCase())) {
                delete request.headers[name];
            }
        }
        if (new window.URL(request.url, window.location.href).origin === window.location.origin) {
            request.credentials = 'same-origin';
            request.headers['X-Surok-Swagger'] = 'true';
            request.headers['X-Requested-With'] = 'XMLHttpRequest';
            if (authorization) {
                request.headers.Authorization = authorization;
            }
            if (token) {
                request.headers['X-XSRF-TOKEN'] = token;
            }
        } else {
            request.credentials = 'omit';
        }
        return request;
    }

    /** Keeps the masked token from initial documentation and subsequent responses. */
    function response(response) {
        if (new window.URL(response.url, window.location.href).origin === window.location.origin) {
            const header = Object.entries(response.headers || {})
                .find(([name]) => name.toLowerCase() === 'x-xsrf-token');
            if (header) {
                token = header[1];
            }
        }
        return response;
    }

    return { plugin, request, response };
})();

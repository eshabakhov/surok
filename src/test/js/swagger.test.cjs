const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const { test } = require('node:test');

/** Builds an isolated Swagger runtime with controllable HTTP responses. */
function runtime(fetch) {
    const state = { authorized: false, documents: [], errors: [] };
    const context = {
        window: {
            fetch,
            URL,
            TextEncoder,
            btoa,
            location: { href: 'https://school.test/app/swagger-ui/index.html', origin: 'https://school.test' }
        }
    };
    vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/swagger.js', 'utf8'), context);
    const system = {
        getConfigs: () => ({ url: '/app/api-docs' }),
        specSelectors: { url: () => '/app/api-docs', specJson: () => ({ toJS: () => ({ openapi: '3.0.1', paths: { secret: {} } }) }) },
        specActions: { updateSpec: spec => state.documents.push(JSON.parse(spec)) },
        errActions: { newAuthErr: error => state.errors.push(error), clear: () => {} },
        authActions: { logout: () => { state.authorized = false; } }
    };
    const plugin = context.window.surokSwagger.plugin();
    state.authorize = plugin.statePlugins.auth.wrapActions.authorize(
        () => { state.authorized = true; }, system
    );
    state.logout = plugin.statePlugins.auth.wrapActions.logout(
        () => { state.authorized = false; }, system
    );
    state.request = context.window.surokSwagger.request;
    return state;
}

/** Basic credentials supplied by the Swagger dialog. */
function credentials() {
    return { 'Basic auth': { value: { username: 'директор', password: 'пароль' } } };
}

/** Successful OpenAPI response with a CSRF token. */
function response(paths) {
    return { ok: true, headers: new Headers({ 'X-XSRF-TOKEN': 'masked-token' }), json: async () => ({ openapi: '3.0.1', paths }) };
}

test('Authorize waits for the server before accepting credentials', async () => {
    let finish;
    const state = runtime(() => new Promise(resolve => { finish = resolve; }));
    const pending = state.authorize(credentials());
    const before = state.authorized;
    finish(response({ catalogue: {} }));
    await pending;
    assert.deepEqual([before, state.authorized, state.documents.at(-1).paths], [false, true, { catalogue: {} }]);
});

test('Authorize rejects a wrong password and hides protected documentation', async () => {
    const state = runtime(async () => ({ ok: false, status: 401 }));
    await state.authorize(credentials());
    assert.deepEqual([state.authorized, state.documents.at(-1).paths, state.errors.length], [false, {}, 1]);
});

test('Logout loads the public description', async () => {
    const state = runtime(async () => response({ registration: {} }));
    await state.logout(['Basic auth']);
    assert.deepEqual([state.authorized, state.documents.at(-1).paths], [false, { registration: {} }]);
});

test('An old authorization response cannot restore privileges after Logout', async () => {
    let finish;
    let calls = 0;
    const state = runtime(() => ++calls === 1
        ? new Promise(resolve => { finish = resolve; }) : Promise.resolve(response({ public: {} })));
    const pending = state.authorize(credentials());
    await state.logout(['Basic auth']);
    finish(response({ administration: {} }));
    await pending;
    assert.deepEqual([state.authorized, state.documents.at(-1).paths], [false, { public: {} }]);
});

test('Try it out sends verified UTF-8 credentials and the masked CSRF token', async () => {
    const state = runtime(async () => response({ catalogue: {} }));
    await state.authorize(credentials());
    const request = state.request({ url: 'https://school.test/app/federal/subjects', headers: {} });
    assert.deepEqual(
        [request.headers.Authorization, request.headers['X-XSRF-TOKEN'], request.headers['X-Surok-Swagger']],
        [`Basic ${Buffer.from('директор:пароль').toString('base64')}`, 'masked-token', 'true']
    );
});

test('Credentials and CSRF tokens never leave the application origin', async () => {
    const state = runtime(async () => response({ catalogue: {} }));
    await state.authorize(credentials());
    const request = state.request({ url: 'https://external.test/api', headers: { Authorization: 'old-secret' } });
    assert.deepEqual(Object.keys(request.headers), []);
});

test('Network failures do not retain authorized operations', async () => {
    const state = runtime(async () => { throw new Error('Offline'); });
    await state.authorize(credentials());
    assert.deepEqual([state.authorized, state.documents.at(-1).paths, state.errors.length], [false, {}, 1]);
});

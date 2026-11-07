import { test as base, request as requestFactory, type APIRequestContext } from '@playwright/test';
export { expect } from '@playwright/test';
async function login(client: APIRequestContext) {
  let token = await (await client.get('/api/v1/auth/csrf')).json();
  const response = await client.post('/api/v1/auth/login', { form: { username: 'synthetic-hr', password: process.env.EMS_E2E_PASSWORD! }, headers: { [token.headerName]: token.token } });
  if (response.status() !== 204) throw new Error('Synthetic HR login failed');
  token = await (await client.get('/api/v1/auth/csrf')).json();
  return token;
}
export const test = base.extend({
  request: async ({ baseURL }, use) => {
    const initial = await requestFactory.newContext({ baseURL });
    const token = await login(initial);
    const client = await requestFactory.newContext({ baseURL, storageState: await initial.storageState(), extraHTTPHeaders: { [token.headerName]: token.token } });
    await initial.dispose();
    try { await use(client); } finally { await client.dispose(); }
  },
  page: async ({ page }, use) => { await login(page.context().request); await use(page); },
});

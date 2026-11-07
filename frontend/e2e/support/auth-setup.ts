import { request } from '@playwright/test';
export default async function setup() {
  const client = await request.newContext({ baseURL: 'http://127.0.0.1:5173' });
  try {
    let token = await (await client.get('/api/v1/auth/csrf')).json();
    const login = await client.post('/api/v1/auth/login', { form: { username: 'synthetic-admin', password: process.env.EMS_E2E_PASSWORD! }, headers: { [token.headerName]: token.token } });
    if (login.status() !== 204) throw new Error('Synthetic bootstrap login failed');
    token = await (await client.get('/api/v1/auth/csrf')).json();
    for (const role of ['HR', 'MANAGER', 'EMPLOYEE']) {
      const created = await client.post('/api/v1/admin/accounts', { data: { login: `synthetic-${role.toLowerCase()}`, password: process.env.EMS_E2E_PASSWORD, role }, headers: { [token.headerName]: token.token } });
      if (created.status() !== 201) throw new Error('Synthetic account provisioning failed');
    }
    for (const viewport of ['small-phone', 'iphone-size', 'android-size', 'tablet', 'desktop']) {
      const created = await client.post('/api/v1/admin/accounts', { data: { login: `throttle-${viewport}`, password: process.env.EMS_E2E_PASSWORD, role: 'HR' }, headers: { [token.headerName]: token.token } });
      if (created.status() !== 201) throw new Error('Synthetic throttle account provisioning failed');
    }
  } finally { await client.dispose(); }
}

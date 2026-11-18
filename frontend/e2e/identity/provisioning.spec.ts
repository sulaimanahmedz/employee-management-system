import { test, expect } from '@playwright/test';

test.describe('Administrative account provisioning', () => {
  test('creates a least-privilege account, rejects duplicates and allows the new account to sign in', async ({ page, browser }, info) => {
    await page.goto('/');
    await page.getByLabel('Login',{exact:true}).fill('synthetic-admin');
    await page.getByLabel('Password',{exact:true}).fill(process.env.EMS_E2E_PASSWORD!);
    await page.getByRole('button',{name:'Sign in',exact:true}).click();
    await expect(page.getByRole('heading',{name:'Create account'})).toBeVisible();
    const login=`provision-${info.project.name}`;
    await page.getByLabel('Account login').fill(login);
    await page.getByLabel('Initial password').fill('short');
    await page.getByRole('button',{name:'Create account',exact:true}).click();
    await expect(page.getByRole('alert')).toBeVisible();
    await expect(page.getByLabel('Initial password')).toHaveValue('');
    await page.getByLabel('Initial password').fill(process.env.EMS_E2E_PASSWORD!);
    await page.getByRole('button',{name:'Create account',exact:true}).click();
    await expect(page.getByRole('status')).toHaveText(`Created ${login} with role EMPLOYEE.`);
    await expect(page.getByLabel('Initial password')).toHaveValue('');
    await page.getByLabel('Account login').fill(login);
    await page.getByLabel('Initial password').fill(process.env.EMS_E2E_PASSWORD!);
    await page.getByRole('button',{name:'Create account',exact:true}).click();
    await expect(page.getByRole('alert')).toHaveText('Account login is unavailable');
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
    const other=await browser.newContext({baseURL:'http://127.0.0.1:5173',viewport:page.viewportSize()!});
    try {
      const member=await other.newPage(); await member.goto('/');
      await member.getByLabel('Login',{exact:true}).fill(login);
      await member.getByLabel('Password',{exact:true}).fill(process.env.EMS_E2E_PASSWORD!);
      await member.getByRole('button',{name:'Sign in',exact:true}).click();
      await expect(member.getByRole('heading',{name:'Access denied'})).toBeVisible();
      await expect(member.getByRole('button',{name:'Create account',exact:true})).toHaveCount(0);
      const csrf=await (await other.request.get('/api/v1/auth/csrf')).json();
      const forbidden=await other.request.post('/api/v1/admin/accounts',{headers:{[csrf.headerName]:csrf.token},data:{login:'escalation-attempt',password:process.env.EMS_E2E_PASSWORD!,role:'ADMIN'}});
      expect(forbidden.status()).toBe(403);
      expect((await other.request.get('/api/v1/admin/accounts')).status()).toBe(403);
      await member.getByRole('button',{name:'Sign out'}).click();
    } finally {await other.close();}
    await page.getByRole('button',{name:'Sign out'}).click();
    await expect(page.getByRole('heading',{name:'Sign in',exact:true})).toBeVisible();
  });
  for (const role of ['ADMIN', 'HR', 'MANAGER']) {
    test(`selected ${role} role receives only its intended workspace`, async ({ page }, info) => {
      await page.goto('/');
      await page.getByLabel('Login', { exact: true }).fill('synthetic-admin');
      await page.getByLabel('Password', { exact: true }).fill(process.env.EMS_E2E_PASSWORD!);
      await page.getByRole('button', { name: 'Sign in', exact: true }).click();
      await expect(page.getByRole('heading', { name: 'Create account' })).toBeVisible();
      const login = `role-${role.toLowerCase()}-${info.project.name}`;
      await page.getByLabel('Account login').fill(login);
      await page.getByLabel('Initial password').fill(process.env.EMS_E2E_PASSWORD!);
      await page.getByLabel('Account role').selectOption(role);
      await page.getByRole('button', { name: 'Create account', exact: true }).click();
      await expect(page.getByRole('status')).toHaveText(`Created ${login} with role ${role}.`);
      await expect(page.getByLabel('Account role')).toHaveValue('EMPLOYEE');
      await page.getByRole('button', { name: 'Sign out' }).click();
      await expect(page.getByRole('heading', { name: 'Sign in', exact: true })).toBeVisible();
      await page.getByLabel('Login', { exact: true }).fill(login);
      await page.getByLabel('Password', { exact: true }).fill(process.env.EMS_E2E_PASSWORD!);
      await page.getByRole('button', { name: 'Sign in', exact: true }).click();
      const heading = role === 'ADMIN' ? 'Create account' : role === 'HR' ? 'Your people, connected.' : 'Access denied';
      await expect(page.getByRole('heading', { name: heading })).toBeVisible();
      const identity = await (await page.request.get('/api/v1/auth/session')).json();
      expect(identity.authorities).toEqual([`ROLE_${role}`]);
      expect((await page.request.get('/api/v1/employees')).status()).toBe(role === 'HR' ? 200 : 403);
      if (role !== 'ADMIN') {
        await expect(page.getByRole('button', { name: 'Create account', exact: true })).toHaveCount(0);
        const token = await (await page.request.get('/api/v1/auth/csrf')).json();
        expect((await page.request.post('/api/v1/admin/accounts', {
          headers: { [token.headerName]: token.token },
          data: { login: `forbidden-${login}`, password: process.env.EMS_E2E_PASSWORD!, role: 'ADMIN' },
        })).status()).toBe(403);
      }
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
      await page.getByRole('button', { name: 'Sign out' }).click();
      await expect(page.getByRole('heading', { name: 'Sign in', exact: true })).toBeVisible();
    });
  }
});

import { test, expect } from '@playwright/test';

test.describe('Login abuse controls', () => {
  test('limits repeated attempts across browser sessions and leaves another account usable', async ({ page, browser }, info) => {
    const login = `throttle-${info.project.name}`;
    await page.goto('/');
    await page.getByLabel('Login', { exact: true }).fill(login);
    for (let i = 0; i < 5; i++) {
      await page.getByLabel('Password', { exact: true }).fill('incorrect-synthetic-password');
      await page.getByRole('button', {name:'Sign in',exact:true}).click();
      await expect(page.getByLabel('Password', {exact:true})).toHaveValue('');
      await expect(page.getByRole('alert')).toContainText('wait a minute');
    }
    const fresh = await browser.newContext({ baseURL: 'http://127.0.0.1:5173' });
    try {
      const other = await fresh.newPage(); await other.goto('/');
      await other.getByLabel('Login', {exact:true}).fill(login.toUpperCase());
      await other.getByLabel('Password', {exact:true}).fill(process.env.EMS_E2E_PASSWORD!);
      await other.getByRole('button', {name:'Sign in',exact:true}).click();
      await expect(other.getByRole('alert')).toContainText('Unable to sign in');
      expect((await fresh.request.get('/api/v1/auth/session')).status()).toBe(401);
    } finally { await fresh.close(); }
    await page.getByLabel('Login', {exact:true}).fill('synthetic-hr');
    await page.getByLabel('Password', {exact:true}).fill(process.env.EMS_E2E_PASSWORD!);
    await page.getByRole('button', {name:'Sign in',exact:true}).click();
    await expect(page.getByRole('heading',{name:'Your people, connected.'})).toBeVisible();
    await page.getByRole('button',{name:'Sign out'}).click();
    await expect(page.getByRole('heading',{name:'Sign in',exact:true})).toBeVisible();
  });
});

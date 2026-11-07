import { test, expect } from '@playwright/test';

test.describe('Authenticated sessions and least-privilege access', () => {
  test('rejects invalid credentials, signs in HR and revokes the session on logout', async ({ page, request }) => {
    expect((await request.get('/api/v1/employees')).status()).toBe(401);
    expect((await request.post('/api/v1/auth/login', { form: { username: 'synthetic-hr', password: process.env.EMS_E2E_PASSWORD! } })).status()).toBe(403);
    await page.goto('/');
    await page.getByLabel('Login', { exact: true }).fill('synthetic-hr');
    await page.getByLabel('Password', { exact: true }).fill('incorrect-synthetic-password');
    await page.getByRole('button', { name: 'Sign in', exact: true }).click();
    await expect(page.getByRole('alert')).toContainText('Unable to sign in');
    await expect(page.getByLabel('Password', { exact: true })).toHaveValue('');
    await page.getByLabel('Password', { exact: true }).fill(process.env.EMS_E2E_PASSWORD!);
    await page.getByRole('button', { name: 'Sign in', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Your people, connected.' })).toBeVisible();
    expect((await page.context().request.get('/api/v1/employees')).status()).toBe(200);
    await page.reload();
    await expect(page.getByRole('button', { name: 'Sign out' })).toBeVisible();
    await page.getByRole('button', { name: 'Sign out' }).click();
    await expect(page.getByRole('heading', { name: 'Sign in', exact: true })).toBeVisible();
    expect((await page.context().request.get('/api/v1/employees')).status()).toBe(401);
  });
  for (const role of ['admin', 'manager', 'employee']) {
    test(`${role} cannot access employee data or overflow the login layout`, async ({ page }) => {
      await page.goto('/');
      await page.getByLabel('Login', { exact: true }).fill(`synthetic-${role}`);
      await page.getByLabel('Password', { exact: true }).fill(process.env.EMS_E2E_PASSWORD!);
      await page.getByRole('button', { name: 'Sign in', exact: true }).click();
      await expect(page.getByRole('heading', { name: 'Access denied' })).toBeVisible();
      expect((await page.context().request.get('/api/v1/employees/1/documents')).status()).toBe(403);
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBeTruthy();
      await page.getByRole('button', { name: 'Sign out' }).click();
      await expect(page.getByRole('heading', { name: 'Sign in', exact: true })).toBeVisible();
    });
  }
});

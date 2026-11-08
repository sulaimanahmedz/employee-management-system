import { test, expect } from '@playwright/test';

test.describe('Session and connection recovery', () => {
  test('clears credentials after connection loss and signs in after connectivity returns', async ({ page, context }) => {
    await page.goto('/');
    await page.getByLabel('Login', { exact: true }).fill('synthetic-hr');
    await page.getByLabel('Password', { exact: true }).fill(process.env.EMS_E2E_PASSWORD!);
    await context.setOffline(true);
    try {
      await page.getByRole('button', { name: 'Sign in', exact: true }).click();
      await expect(page.getByRole('alert')).toContainText('Cannot reach the server');
      await expect(page.getByLabel('Password', { exact: true })).toHaveValue('');
    } finally { await context.setOffline(false); }
    await page.getByLabel('Password', { exact: true }).fill(process.env.EMS_E2E_PASSWORD!);
    await page.getByRole('button', { name: 'Sign in', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Your people, connected.' })).toBeVisible();
    await page.getByRole('button', { name: 'Sign out' }).click();
    await expect(page.getByRole('heading', { name: 'Sign in', exact: true })).toBeVisible();
  });

  test('removes a stale workspace after another tab signs out and obtains fresh CSRF on reauthentication', async ({ page, context }) => {
    await page.goto('/');
    await page.getByLabel('Login', { exact: true }).fill('synthetic-hr');
    await page.getByLabel('Password', { exact: true }).fill(process.env.EMS_E2E_PASSWORD!);
    await page.getByRole('button', { name: 'Sign in', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Your people, connected.' })).toBeVisible();
    const otherTab = await context.newPage();
    try {
      await otherTab.goto('/');
      await otherTab.getByRole('button', { name: 'Sign out' }).click();
      await expect(otherTab.getByRole('heading', { name: 'Sign in', exact: true })).toBeVisible();
      await page.getByRole('button', { name: 'Apply filters', exact: true }).click();
      await expect(page.getByRole('alert')).toContainText('Your session has expired');
      await expect(page.getByRole('heading', { name: 'Your people, connected.' })).toHaveCount(0);
      await expect(page.getByLabel('Password', { exact: true })).toHaveValue('');
      await page.getByLabel('Password', { exact: true }).fill(process.env.EMS_E2E_PASSWORD!);
      await page.getByRole('button', { name: 'Sign in', exact: true }).click();
      await expect(page.getByRole('heading', { name: 'Your people, connected.' })).toBeVisible();
      expect((await context.request.get('/api/v1/employees')).status()).toBe(200);
      await page.getByRole('button', { name: 'Sign out' }).click();
      await expect(page.getByRole('heading', { name: 'Sign in', exact: true })).toBeVisible();
    } finally { await otherTab.close(); }
  });
});

import { test, expect } from '@playwright/test';
import { employee } from '../support/employees';

test.describe('Employee profiles', () => {
test('profile edits do not duplicate related sections', async ({ page, request }) => {
  const id = await employee(request);
  try {
    await page.goto('/');
    await page.getByRole('button', { name: 'View profile for Mobile Test' }).click();
    for (const title of ['Senior Engineer', 'Lead Engineer']) {
      await page.getByRole('button', { name: 'Edit profile', exact: true }).click();
      await page.getByLabel('Job title', { exact: true }).fill(title);
      await page.getByRole('button', { name: 'Save changes', exact: true }).click();
      await expect(page.getByText('Employee profile updated successfully.')).toBeVisible();
      for (const name of ['Leave requests', 'Onboarding checklist', 'Employment history', 'Document register']) {
        await expect(page.getByRole('region', { name, exact: true })).toHaveCount(1);
      }
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
    }
  } finally { await request.delete(`/api/v1/employees/${id}`); }
});

});

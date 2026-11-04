import { test, expect } from '@playwright/test';
import { employee } from '../support/employees';

test.describe('Employee directory', () => {
test('directory filters and create form fit small screens', async ({ page }) => {
  await page.goto('/');
  await page.getByLabel('Search employees').fill('Missing employee');
  await page.getByRole('button', { name: 'Apply filters', exact: true }).click();
  await page.getByRole('button', { name: 'Clear filters', exact: true }).click();
  await page.getByRole('button', { name: '+ Add employee', exact: true }).click();
  for (const name of ['First name', 'Last name', 'Work email', 'Department', 'Job title']) {
    const box = await page.getByLabel(name, { exact: true }).boundingBox();
    expect(box!.x).toBeGreaterThanOrEqual(0);
    expect(box!.x + box!.width).toBeLessThanOrEqual(page.viewportSize()!.width + 1);
  }
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
});
});

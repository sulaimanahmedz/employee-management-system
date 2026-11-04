import { expect, test } from '@playwright/test';
import { createProfileEmployee } from '../support/profile-fixtures';
test.describe('Directory search and navigation', () => {
    test(`filters synthetic records and preserves search on profile return`, async ({ page, request }) => {
      const width = page.viewportSize()!.width;
      const employee = await createProfileEmployee(request, `Directory${width}`);
      try {
        await page.goto('/');
        await page.getByRole('searchbox', { name: 'Search employees' }).fill(employee.email);
        await page.getByRole('combobox', { name: 'Employee status', exact: true }).selectOption('INACTIVE');
        await page.getByRole('button', { name: 'Apply filters', exact: true }).click();
        await expect(page.getByRole('heading', { name: 'No employees match your filters' })).toBeVisible();
        await page.getByRole('combobox', { name: 'Employee status', exact: true }).selectOption('ACTIVE');
        await page.getByRole('combobox', { name: 'Sort employees by', exact: true }).selectOption('name');
        await page.getByRole('combobox', { name: 'Sort direction', exact: true }).selectOption('desc');
        await page.getByRole('button', { name: 'Apply filters', exact: true }).click();
        await expect(page.getByText('Showing 1–1 of 1')).toBeVisible();
        await page.getByRole('button', { name: `View profile for Synthetic Directory${width}` }).click();
        await expect(page.getByRole('heading', { name: `Synthetic Directory${width}`, exact: true })).toBeVisible();
        await page.getByRole('button', { name: 'Back to directory', exact: true }).click();
        await expect(page.getByRole('searchbox', { name: 'Search employees' })).toHaveValue(employee.email);
        await expect(page.getByText(/Sort: Last name, first name \(descending\)/)).toBeVisible();
        await expect(page.getByText('Showing 1–1 of 1')).toBeVisible();
        await page.getByRole('button', { name: 'Clear filters', exact: true }).click();
        await expect(page.getByRole('searchbox', { name: 'Search employees' })).toHaveValue('');
        await expect(page.getByRole('combobox', { name: 'Sort employees by', exact: true })).toHaveValue('id');
        await expect(page.getByRole('button', { name: `View profile for Synthetic Directory${width}` })).toBeVisible();
      } finally {
        expect((await request.delete(`/api/v1/employees/${employee.id}`)).status()).toBe(204);
      }
    });
});

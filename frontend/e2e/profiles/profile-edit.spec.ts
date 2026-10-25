import { expect, test } from '@playwright/test';
import { createProfileEmployee } from '../support/profile-fixtures';
test.describe('Profile editing and conflict recovery', () => {
  for (const width of [320, 390, 412, 768, 1440]) {
    test(`retains failed edits and saves corrected data at ${width}px`, async ({ page, request }) => {
      await page.setViewportSize({ width, height: 1000 });
      const employee = await createProfileEmployee(request, `Edit${width}`);
      let otherId: number | undefined;
      try {
        const other = await createProfileEmployee(request, `Other${width}`); otherId = other.id;
        await page.goto('/');
        await page.getByRole('button', { name: `View profile for Synthetic Edit${width}` }).click();
        await page.getByRole('button', { name: 'Edit profile', exact: true }).click();
        await page.getByLabel('Work email', { exact: true }).fill(other.email);
        await page.getByLabel('Job title', { exact: true }).fill('Senior Engineer');
        await page.getByRole('button', { name: 'Save changes', exact: true }).click();
        await expect(page.getByRole('alert')).toBeVisible();
        await expect(page.getByLabel('Work email', { exact: true })).toHaveValue(other.email);
        await expect(page.getByLabel('Job title', { exact: true })).toHaveValue('Senior Engineer');
        const failed = await request.get(`/api/v1/employees/${employee.id}`);
        expect(await failed.json()).toMatchObject({ email: employee.email, jobTitle: 'Engineer' });
        await page.getByLabel('Work email', { exact: true }).fill(employee.email);
        await page.getByRole('button', { name: 'Save changes', exact: true }).click();
        await expect(page.getByText('Employee profile updated successfully.')).toBeVisible();
        const persisted = await request.get(`/api/v1/employees/${employee.id}`);
        expect(await persisted.json()).toMatchObject({ email: employee.email, jobTitle: 'Senior Engineer' });
      } finally {
        if (otherId !== undefined) expect((await request.delete(`/api/v1/employees/${otherId}`)).status()).toBe(204);
        expect((await request.delete(`/api/v1/employees/${employee.id}`)).status()).toBe(204);
      }
    });
  }
});

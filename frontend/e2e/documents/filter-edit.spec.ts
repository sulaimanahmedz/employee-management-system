import { expect, test } from '@playwright/test';
import { createProfileEmployee } from '../support/profile-fixtures';
test.describe('Document renewal filtering and metadata edits', () => {
  for (const width of [320, 390, 412, 768, 1440]) {
    test(`updates filter membership and removes metadata at ${width}px`, async ({ page, request }) => {
      await page.setViewportSize({ width, height: 1000 });
      const employee = await createProfileEmployee(request, `Documents${width}`);
      const path = `/api/v1/employees/${employee.id}/documents`;
      try {
        const seed = await request.post(path, { data: { name: 'Synthetic contract', category: 'CONTRACT', expiresOn: '2026-12-01' } });
        expect(seed.status()).toBe(201); const document = await seed.json();
        await page.goto('/'); await page.getByRole('button', { name: `View profile for Synthetic Documents${width}` }).click();
        const register = page.getByRole('region', { name: 'Document register', exact: true });
        await expect(register.getByText('Synthetic contract', { exact: true })).toBeVisible();
        await register.getByLabel('Document name', { exact: true }).fill('   ');
        await expect(register.getByRole('button', { name: 'Add metadata', exact: true })).toBeDisabled();
        await register.getByLabel('Document name', { exact: true }).fill('');
        await register.getByRole('searchbox', { name: 'Search document names' }).fill('synthetic');
        await register.getByRole('combobox', { name: 'Document category', exact: true }).selectOption('CONTRACT');
        await register.getByLabel('Expires on or before', { exact: true }).fill('2026-12-31');
        await register.getByRole('button', { name: 'Apply document filters', exact: true }).click();
        await expect(register.getByText('1 matching document records')).toBeVisible();
        await register.getByRole('button', { name: 'Edit Synthetic contract', exact: true }).click();
        await register.getByLabel('Expiry date (optional)', { exact: true }).fill('2027-12-01');
        await register.getByRole('button', { name: 'Save metadata changes', exact: true }).click();
        await expect(register.getByText('Document metadata updated.')).toBeVisible();
        await expect(register.getByText('No document metadata matches these filters.')).toBeVisible();
        const persisted = await request.get(`${path}/${document.id}`); expect(await persisted.json()).toMatchObject({ id: document.id, expiresOn: '2027-12-01' });
        await register.getByRole('button', { name: 'Clear document filters', exact: true }).click();
        await expect(register.getByText('Synthetic contract', { exact: true })).toBeVisible();
        page.once('dialog', dialog => dialog.dismiss()); await register.getByRole('button', { name: 'Remove Synthetic contract', exact: true }).click();
        await expect(register.getByText('Synthetic contract', { exact: true })).toBeVisible();
        page.once('dialog', dialog => dialog.accept()); await register.getByRole('button', { name: 'Remove Synthetic contract', exact: true }).click();
        await expect(register.getByText('No document metadata registered.')).toBeVisible();
        expect((await request.get(`${path}/${document.id}`)).status()).toBe(404);
      } finally { expect((await request.delete(`/api/v1/employees/${employee.id}`)).status()).toBe(204); }
    });
  }
});

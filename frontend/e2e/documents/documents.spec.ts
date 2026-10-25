import { test, expect } from '@playwright/test';
import { employee } from '../support/employees';

test.describe('Document register', () => {
test('document actions sit below long names and remain usable without page overflow', async ({ page, request }) => {
  const id = await employee(request);
  try {
    const name = 'Employment-contract-' + 'VeryLongName'.repeat(8);
    expect((await request.post(`/api/v1/employees/${id}/documents`, { data: { name, category: 'CONTRACT', expiresOn: null } })).status()).toBe(201);
    await page.goto('/');
    await page.getByRole('button', { name: 'View profile for Mobile Test' }).click();
    const section = page.getByRole('region', { name: 'Document register' });
    const title = section.getByText(name, { exact: true });
    await expect(title).toBeVisible();
    const edit = section.getByRole('button', { name: 'Edit ' + name, exact: true });
    const titleBox = await title.boundingBox();
    const editBox = await edit.boundingBox();
    expect(editBox!.y).toBeGreaterThanOrEqual(titleBox!.y + titleBox!.height);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
    await edit.click();
    await section.getByLabel('Document name', { exact: true }).fill('Renewed contract');
    await section.getByRole('button', { name: 'Save metadata changes' }).click();
    await expect(section.getByText('Renewed contract', { exact: true })).toBeVisible();
    page.on('dialog', d => d.accept());
    await section.getByRole('button', { name: 'Remove Renewed contract', exact: true }).click();
    await expect(section.getByText('No document metadata registered.')).toBeVisible();
  } finally { await request.delete(`/api/v1/employees/${id}`); }
});

});

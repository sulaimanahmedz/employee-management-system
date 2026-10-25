import { expect, test } from '@playwright/test';

test.describe('Employment history', () => {
  for (const width of [320, 390, 412, 768, 1440]) {
    test(`shows persisted changes and filters events at ${width}px`, async ({ page, request }) => {
      await page.setViewportSize({ width, height: 1000 });
      const employee = { firstName: 'Synthetic', lastName: `History${width}`, email: `history.${width}@example.com`,
        department: 'IT', jobTitle: 'Engineer', status: 'ACTIVE' };
      const created = await request.post('/api/v1/employees', { data: employee });
      expect(created.status()).toBe(201);
      const { id } = await created.json();
      const path = `/api/v1/employees/${id}`;
      try {
        expect((await request.put(path, { data: { ...employee, jobTitle: 'Senior Engineer' } })).status()).toBe(200);
        await page.goto('/');
        await page.getByRole('button', { name: `View profile for Synthetic History${width}` }).click();
        const history = page.getByRole('region', { name: 'Employment history', exact: true });
        await expect(history.getByText('2 matching history events')).toBeVisible();
        await expect(history.getByText('IT · Senior Engineer · Active')).toBeVisible();
        await history.getByLabel('Show history events').selectOption('CHANGED');
        await expect(history.getByText('1 matching history events')).toBeVisible();
        await expect(history.getByText('IT · Engineer · Active', { exact: true })).toHaveCount(0);
        await history.getByLabel('Show history events').selectOption('BASELINE');
        await expect(history.getByText('No history events match this type.')).toBeVisible();
        await history.getByLabel('Show history events').selectOption('');
        await expect(history.getByText('2 matching history events')).toBeVisible();
        expect((await request.get(path + '/history?kind=INVALID')).status()).toBe(400);
      } finally {
        expect((await request.delete(path)).status()).toBe(204);
      }
    });
  }
});

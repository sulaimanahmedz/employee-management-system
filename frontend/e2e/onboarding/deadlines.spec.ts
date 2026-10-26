import { expect, test } from '@playwright/test';
import { createProfileEmployee } from '../support/profile-fixtures';
test.describe('Onboarding deadline review and removal', () => {
  for (const width of [320, 390, 412, 768, 1440]) {
    test(`filters, reopens and removes tasks at ${width}px`, async ({ page, request }) => {
      await page.setViewportSize({ width, height: 1000 });
      const employee = await createProfileEmployee(request, `Onboarding${width}`);
      const path = `/api/v1/employees/${employee.id}/onboarding-tasks`;
      try {
        const seed = await request.post(path, { data: { title: 'Synthetic induction', owner: 'HR', dueOn: '2026-12-01' } });
        expect(seed.status()).toBe(201); const task = await seed.json();
        expect((await request.post(path, { data: { title: ' ', owner: 'HR', dueOn: null } })).status()).toBe(400);
        await page.goto('/'); await page.getByRole('button', { name: `View profile for Synthetic Onboarding${width}` }).click();
        const section = page.getByRole('region', { name: 'Onboarding checklist', exact: true });
        await expect(section.getByText('Synthetic induction', { exact: true })).toBeVisible();
        await section.getByLabel('Tasks due on or before', { exact: true }).fill('2026-11-30');
        await expect(section.getByText('No tasks match these filters.')).toBeVisible();
        await section.getByLabel('Tasks due on or before', { exact: true }).fill('2026-12-01');
        await expect(section.getByText('Synthetic induction', { exact: true })).toBeVisible();
        await section.getByRole('combobox', { name: 'Show onboarding tasks', exact: true }).selectOption('false');
        await section.getByRole('button', { name: 'Complete Synthetic induction', exact: true }).click();
        await expect(section.getByText('No tasks match these filters.')).toBeVisible();
        await expect(section.getByText('1 of 1 tasks complete · 0 matching tasks')).toBeVisible();
        await section.getByRole('combobox', { name: 'Show onboarding tasks', exact: true }).selectOption('true');
        await section.getByRole('button', { name: 'Reopen Synthetic induction', exact: true }).click();
        await expect(section.getByText('No tasks match these filters.')).toBeVisible();
        await section.getByRole('combobox', { name: 'Show onboarding tasks', exact: true }).selectOption('');
        await section.getByRole('button', { name: 'Clear due-date filter', exact: true }).click();
        await expect(section.getByText('Synthetic induction', { exact: true })).toBeVisible();
        expect(await (await request.get(`${path}/${task.id}`)).json()).toMatchObject({ completed: false, dueOn: '2026-12-01' });
        page.once('dialog', dialog => dialog.dismiss()); await section.getByRole('button', { name: 'Remove task Synthetic induction', exact: true }).click();
        await expect(section.getByText('Synthetic induction', { exact: true })).toBeVisible();
        page.once('dialog', dialog => dialog.accept()); await section.getByRole('button', { name: 'Remove task Synthetic induction', exact: true }).click();
        await expect(section.getByText('No onboarding tasks yet.')).toBeVisible(); expect((await request.get(`${path}/${task.id}`)).status()).toBe(404);
      } finally { expect((await request.delete(`/api/v1/employees/${employee.id}`)).status()).toBe(204); }
    });
  }
});

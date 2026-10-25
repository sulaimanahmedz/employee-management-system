import { expect, test } from '@playwright/test';

test.describe('Onboarding checklist', () => {
test('adds, edits and completes a task through the real application', async ({ page, request }) => {
  const created = await request.post('/api/v1/employees', { data: {
    firstName: 'Synthetic', lastName: 'Playwright', email: 'playwright@example.com',
    department: 'IT', jobTitle: 'Engineer', status: 'ACTIVE',
  } });
  expect(created.status()).toBe(201);
  const employee = await created.json();
  const path = `/api/v1/employees/${employee.id}`;
  try {
    await page.goto('/');
    await page.getByRole('button', { name: 'View profile for Synthetic Playwright' }).click();
    const section = page.getByRole('region', { name: 'Onboarding checklist' });
    await expect(section.getByText('No onboarding tasks yet.')).toBeVisible();
    await section.getByLabel('Task title').fill(' ');
    await section.getByLabel('Owner label').fill('IT');
    await expect(section.getByRole('button', { name: 'Add task', exact: true })).toBeDisabled();
    await section.getByLabel('Task title').fill('Prepare laptop');
    await section.getByRole('button', { name: 'Add task', exact: true }).click();
    await expect(section.getByText('Prepare laptop', { exact: true })).toBeVisible();
    await section.getByRole('button', { name: 'Edit task Prepare laptop' }).click();
    await section.getByLabel('Task title', { exact: true }).fill('Prepare workstation');
    await section.getByRole('button', { name: 'Save task changes' }).click();
    await expect(section.getByText('Prepare workstation', { exact: true })).toBeVisible();
    await section.getByRole('button', { name: 'Complete Prepare workstation', exact: true }).click();
    await expect(section.getByText('1 of 1 tasks complete · 1 matching tasks')).toBeVisible();
    await section.getByLabel('Show onboarding tasks').selectOption('false');
    await expect(section.getByText('No tasks match these filters.')).toBeVisible();
    const response = await request.get(path + '/onboarding-tasks');
    expect(response.ok()).toBeTruthy();
    expect((await response.json()).items[0]).toMatchObject({ title: 'Prepare workstation', owner: 'IT', completed: true });
  } finally {
    expect((await request.delete(path)).status()).toBe(204);
  }
});

});

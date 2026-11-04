import { expect, test } from '@playwright/test';
import { createProfileEmployee } from '../support/profile-fixtures';

test.describe('Leave requests and overlap recovery', () => {
  for (const width of [320, 390, 412, 768, 1440]) {
    test(`records, rejects overlaps, edits and cancels at ${width}px`, async ({ page, request }) => {
      await page.setViewportSize({ width, height: 1000 });
      const employee = await createProfileEmployee(request, `Leave${width}`);
      const path = `/api/v1/employees/${employee.id}/leave-requests`;
      try {
        await page.goto('/');
        await page.getByRole('button', { name: `View profile for Synthetic Leave${width}` }).click();
        const leave = page.getByRole('region', { name: 'Leave requests', exact: true });
        await leave.getByLabel('Start date', { exact: true }).fill('2026-12-01');
        await leave.getByLabel('End date', { exact: true }).fill('2026-12-03');
        await leave.getByRole('button', { name: 'Record request', exact: true }).click();
        await expect(leave.getByText('Leave request recorded as pending.')).toBeVisible();
        await expect(leave.getByText('1 matching leave requests')).toBeVisible();
        await leave.getByLabel('Start date', { exact: true }).fill('2026-12-02');
        await leave.getByLabel('End date', { exact: true }).fill('2026-12-04');
        await leave.getByRole('button', { name: 'Record request', exact: true }).click();
        await expect(leave.getByRole('alert')).toBeVisible();
        await expect(leave.getByLabel('End date', { exact: true })).toHaveValue('2026-12-04');
        const afterConflict = await request.get(path); expect((await afterConflict.json()).totalElements).toBe(1);
        page.once('dialog', dialog => dialog.accept());
        await leave.getByRole('button', { name: 'Edit leave request starting 2026-12-01', exact: true }).click();
        await leave.getByLabel('End date', { exact: true }).fill('2026-12-05');
        await leave.getByRole('button', { name: 'Save leave changes', exact: true }).click();
        await expect(leave.getByText('Leave request updated.')).toBeVisible();
        const edited = await request.get(path); expect((await edited.json()).items[0]).toMatchObject({ endOn: '2026-12-05', calendarDays: 5, status: 'PENDING' });
        page.once('dialog', dialog => dialog.accept());
        await leave.getByRole('button', { name: 'Cancel leave request starting 2026-12-01', exact: true }).click();
        await expect(leave.getByText('Leave request cancelled.')).toBeVisible();
        await leave.getByRole('combobox', { name: 'Request status', exact: true }).selectOption('PENDING');
        await leave.getByRole('button', { name: 'Apply leave filters', exact: true }).click();
        await expect(leave.getByText('No leave requests match the applied filters.')).toBeVisible();
        await leave.getByRole('button', { name: 'Clear leave filters', exact: true }).click();
        await expect(leave.getByText('1 matching leave requests')).toBeVisible();
        await expect(leave.getByRole('button', { name: /^Edit leave request starting/ })).toHaveCount(0);
        const cancelled = await request.get(path); expect((await cancelled.json()).items[0].status).toBe('CANCELLED');
      } finally {
        expect((await request.delete(`/api/v1/employees/${employee.id}`)).status()).toBe(204);
      }
    });
  }
});

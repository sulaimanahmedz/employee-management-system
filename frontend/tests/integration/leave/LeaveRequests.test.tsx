import { afterEach, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import LeaveRequests from '../../../src/features/employees/LeaveRequests';
afterEach(() => { cleanup(); vi.unstubAllGlobals(); });
it('maps HTTP overlap conflicts through the real client and preserves the submitted draft', async () => {
  const fetch = vi.fn(async (_url: RequestInfo | URL, options?: RequestInit) => options?.method === 'POST'
    ? new Response(JSON.stringify({ detail: 'Dates overlap a pending request.' }), { status: 409, headers: { 'Content-Type': 'application/problem+json' } })
    : new Response(JSON.stringify({ items: [], page: 0, totalPages: 0, totalElements: 0 }), { status: 200 }));
  vi.stubGlobal('fetch', fetch); const user = userEvent.setup(); render(<LeaveRequests employeeId={7} />);
  await screen.findByText('0 matching leave requests');
  fireEvent.change(screen.getByLabelText('Start date'), { target: { value: '2026-12-01' } });
  fireEvent.change(screen.getByLabelText('End date'), { target: { value: '2026-12-03' } });
  await user.click(screen.getByRole('button', { name: 'Record request' }));
  expect((await screen.findByRole('alert')).textContent).toContain('Dates overlap a pending request.');
  expect((screen.getByLabelText('End date') as HTMLInputElement).value).toBe('2026-12-03');
  expect(fetch).toHaveBeenLastCalledWith('/api/v1/employees/7/leave-requests', expect.objectContaining({ method: 'POST', body: JSON.stringify({ type: 'ANNUAL', startOn: '2026-12-01', endOn: '2026-12-03' }) }));
});

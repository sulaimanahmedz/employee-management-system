import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import LeaveRequests from '../../../src/features/employees/LeaveRequests';
import { api } from '../../../src/lib/api';
vi.mock('../../../src/lib/api', () => ({ api: vi.fn() }));
const call = vi.mocked(api);
const empty = { items: [], page: 0, totalPages: 0, totalElements: 0 };
const pending = { id: 1, type: 'ANNUAL', startOn: '2026-12-01', endOn: '2026-12-01', status: 'PENDING', calendarDays: 1 };
const populated = { ...empty, totalElements: 2, totalPages: 1, items: [pending, { ...pending, id: 2, type: 'UNPAID', status: 'CANCELLED', calendarDays: 2 }] };
const date = (label: string, value: string) => fireEvent.change(screen.getByLabelText(label), { target: { value } });
const button = (name: string) => screen.getByRole('button', { name });
function deferred() { let resolve!: (value: unknown) => void; let reject!: (reason: unknown) => void; const promise = new Promise((yes, no) => { resolve = yes; reject = no; }); return { promise, resolve, reject }; }
beforeEach(() => call.mockReset());
afterEach(cleanup);

it('validates draft dates and applies, summarizes and clears combined filters', async () => {
  call.mockResolvedValue(empty); const user = userEvent.setup(); render(<LeaveRequests employeeId={7} />);
  await screen.findByText('No leave requests match the applied filters.');
  date('Period from', '2026-12-02'); date('Period to', '2026-12-01');
  fireEvent.submit(screen.getByRole('form', { name: 'Filter leave requests' }));
  expect(screen.getByRole('alert').textContent).toContain('Filter end date must'); expect(call).toHaveBeenCalledTimes(1);
  date('Period to', '2026-12-03');
  await user.selectOptions(screen.getByLabelText('Filter leave type'), 'PERSONAL'); await user.selectOptions(screen.getByLabelText('Request status'), 'PENDING');
  await user.click(button('Apply leave filters'));
  await waitFor(() => expect(call).toHaveBeenLastCalledWith('/api/v1/employees/7/leave-requests?page=0&status=PENDING&from=2026-12-02&to=2026-12-03&type=PERSONAL', expect.any(Object)));
  expect(screen.getByText(/Showing pending personal requests/)).toBeTruthy();
  await user.click(button('Clear leave filters')); await waitFor(() => expect(call).toHaveBeenLastCalledWith('/api/v1/employees/7/leave-requests?page=0', expect.any(Object)));
  expect(screen.queryByText(/Showing pending/)).toBeNull();
  fireEvent.submit(screen.getByRole('form', { name: 'Record leave request' })); expect(call).toHaveBeenCalledTimes(3);
  date('Start date', '2026-12-03'); date('End date', '2026-12-01');
  fireEvent.submit(screen.getByRole('form', { name: 'Record leave request' })); expect(screen.getByRole('alert').textContent).toContain('End date must');
});

it('prevents duplicate submissions while saving, then clears the successfully recorded draft', async () => {
  const save = deferred(); call.mockImplementation((_url, options) => options?.method ? save.promise : Promise.resolve(empty));
  const user = userEvent.setup(); render(<LeaveRequests employeeId={7} />); await screen.findByText('0 matching leave requests');
  await user.selectOptions(screen.getByLabelText('Leave type'), 'PERSONAL'); date('Start date', '2026-12-01'); date('End date', '2026-12-02');
  const form = screen.getByRole('form', { name: 'Record leave request' }); fireEvent.submit(form); fireEvent.submit(form);
  expect(call.mock.calls.filter(([, options]) => options?.method)).toHaveLength(1);
  expect(call).toHaveBeenLastCalledWith('/api/v1/employees/7/leave-requests', { method: 'POST', body: JSON.stringify({ type: 'PERSONAL', startOn: '2026-12-01', endOn: '2026-12-02' }) });
  expect((screen.getByLabelText('Start date').closest('fieldset') as HTMLFieldSetElement).disabled).toBe(true);
  await act(async () => save.resolve(pending)); await screen.findByText('Leave request recorded as pending.');
  expect((screen.getByLabelText('Start date') as HTMLInputElement).value).toBe(''); expect((screen.getByLabelText('Leave type') as HTMLSelectElement).value).toBe('ANNUAL');
});

it('requires confirmation to discard drafts, saves edits and confirms cancellation', async () => {
  call.mockResolvedValue(populated); const confirm = vi.spyOn(window, 'confirm'); const user = userEvent.setup(); render(<LeaveRequests employeeId={7} />);
  await screen.findByText('Annual leave'); expect(screen.getByText('Unpaid leave')).toBeTruthy(); expect(screen.getAllByRole('button', { name: /^Edit leave request starting/ })).toHaveLength(1);
  date('End date', '2026-12-05'); confirm.mockReturnValueOnce(false).mockReturnValueOnce(true);
  await user.click(button('Edit leave request starting 2026-12-01')); expect(screen.queryByRole('form', { name: 'Edit leave request' })).toBeNull();
  await user.click(button('Edit leave request starting 2026-12-01')); date('End date', '2026-12-03');
  confirm.mockReturnValueOnce(false).mockReturnValueOnce(true); await user.click(button('Cancel editing')); expect(screen.getByRole('form', { name: 'Edit leave request' })).toBeTruthy();
  await user.click(button('Cancel editing')); expect((screen.getByLabelText('End date') as HTMLInputElement).value).toBe('');
  await user.click(button('Edit leave request starting 2026-12-01')); await user.click(button('Cancel editing'));
  await user.click(button('Edit leave request starting 2026-12-01')); date('End date', '2026-12-02'); await user.click(button('Save leave changes'));
  await screen.findByText('Leave request updated.'); expect(call).toHaveBeenCalledWith('/api/v1/employees/7/leave-requests/1', expect.objectContaining({ method: 'PUT' }));
  confirm.mockReturnValueOnce(false).mockReturnValueOnce(true); const before = call.mock.calls.length;
  await user.click(button('Cancel leave request starting 2026-12-01')); expect(call).toHaveBeenCalledTimes(before);
  await user.click(button('Cancel leave request starting 2026-12-01')); await screen.findByText('Leave request cancelled.');
  expect(call).toHaveBeenCalledWith('/api/v1/employees/7/leave-requests/1/cancellation', { method: 'PUT' });
});

it.each([new Error('Conflict from server'), 'unexpected'])('retains failed save details and permits retry: %s', async cause => {
  let failed = false; call.mockImplementation((_url, options) => { if (!options?.method) return Promise.resolve(empty); if (!failed) { failed = true; return Promise.reject(cause); } return Promise.resolve(pending); });
  const user = userEvent.setup(); render(<LeaveRequests employeeId={7} />); await screen.findByText('0 matching leave requests');
  date('Start date', '2026-12-01'); date('End date', '2026-12-01'); await user.click(button('Record request'));
  expect((await screen.findByRole('alert')).textContent).toContain(cause instanceof Error ? cause.message : 'Unable to save leave request.');
  expect((screen.getByLabelText('Start date') as HTMLInputElement).value).toBe('2026-12-01');
  await user.click(button('Record request')); await screen.findByText('Leave request recorded as pending.'); expect(screen.queryByRole('alert')).toBeNull();
});

it.each([new Error('Load failed'), null])('recovers from load errors via refresh: %s', async cause => {
  call.mockRejectedValueOnce(cause).mockResolvedValue(empty); const user = userEvent.setup(); render(<LeaveRequests employeeId={7} />);
  expect((await screen.findByRole('alert')).textContent).toContain(cause instanceof Error ? cause.message : 'Unable to load leave requests.');
  await user.click(button('Refresh leave requests')); await screen.findByText('0 matching leave requests'); expect(screen.queryByRole('alert')).toBeNull();
});

it.each(['resolve', 'reject'])('ignores a stale employee request that later %ss', async outcome => {
  const old = deferred(); call.mockReturnValueOnce(old.promise).mockResolvedValue(empty); const { rerender } = render(<LeaveRequests employeeId={7} />);
  const signal = call.mock.calls[0][1]?.signal as AbortSignal; rerender(<LeaveRequests employeeId={8} />); expect(signal.aborted).toBe(true);
  await screen.findByText('0 matching leave requests'); await act(async () => { if (outcome === 'resolve') old.resolve(populated); else old.reject(new Error('stale')); });
  expect(screen.queryByText('Annual leave')).toBeNull(); expect(screen.queryByRole('alert')).toBeNull();
});

it('navigates pages and recovers when the final page disappears', async () => {
  let shrinking = false; call.mockImplementation(async url => { const page = Number(new URL(String(url), 'http://localhost').searchParams.get('page')); return { ...empty, totalPages: shrinking ? 1 : 2, totalElements: 11, page, items: page === 1 ? [] : [pending] }; });
  const user = userEvent.setup(); render(<LeaveRequests employeeId={7} />); await screen.findByText('Annual leave');
  await user.click(button('Next requests')); await screen.findByText('No leave requests match the applied filters.');
  await user.click(button('Previous requests')); await screen.findByText('Annual leave'); shrinking = true;
  await user.click(button('Next requests')); await screen.findByText('Annual leave'); await waitFor(() => expect(call).toHaveBeenLastCalledWith('/api/v1/employees/7/leave-requests?page=0', expect.any(Object)));
});

it('summarizes open-ended periods and status-only filters without requiring a type or both dates', async () => {
  call.mockResolvedValue(empty); const user = userEvent.setup(); render(<LeaveRequests employeeId={7} />);
  await screen.findByText('0 matching leave requests');
  date('Period from', '2026-12-01'); await user.click(button('Apply leave filters'));
  await screen.findByText(/Showing all leave requests overlapping 2026-12-01 through any end date/);
  await user.click(button('Clear leave filters')); await user.selectOptions(screen.getByLabelText('Request status'), 'CANCELLED');
  await user.click(button('Apply leave filters'));
  await screen.findByText(/Showing cancelled leave requests overlapping any start date through any end date/);
  expect(call).toHaveBeenLastCalledWith('/api/v1/employees/7/leave-requests?page=0&status=CANCELLED', expect.any(Object));
});

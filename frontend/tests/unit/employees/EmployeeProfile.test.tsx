import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import EmployeeProfile from '../../../src/features/employees/EmployeeProfile';
import { getEmployee, updateEmployee } from '../../../src/features/employees/api';
import type { Employee } from '../../../src/features/employees/types';
vi.mock('../../../src/features/employees/api', () => ({ getEmployee: vi.fn(), updateEmployee: vi.fn() }));
vi.mock('../../../src/features/employees/LeaveRequests', () => ({ default: () => null }));
vi.mock('../../../src/features/employees/OnboardingTasks', () => ({ default: () => null }));
vi.mock('../../../src/features/employees/EmploymentHistory', () => ({ default: () => null }));
vi.mock('../../../src/features/employees/EmployeeDocuments', () => ({ default: () => null }));
const employee: Employee = { id: 7, firstName: 'Synthetic', lastName: 'Profile', email: 'profile@example.com', department: 'IT', jobTitle: 'Engineer', status: 'ACTIVE' };
const get = vi.mocked(getEmployee), update = vi.mocked(updateEmployee);
function deferred() {
  let resolve!: (value: Employee) => void, reject!: (reason: unknown) => void;
  const promise = new Promise<Employee>((yes, no) => { resolve = yes; reject = no; });
  return { promise, resolve, reject };
}
const back = vi.fn(), saved = vi.fn();
beforeEach(() => { get.mockReset().mockResolvedValue(employee); update.mockReset(); back.mockReset(); saved.mockReset(); });
afterEach(() => { cleanup(); vi.restoreAllMocks(); });
async function open() {
  const user = userEvent.setup(); render(<EmployeeProfile employeeId={7} onBack={back} onSaved={saved} />);
  await screen.findByText('Engineer');
  return user;
}
async function edit() { const user = await open(); await user.click(screen.getByRole('button', { name: 'Edit profile' })); return user; }

it('focuses the heading, displays details, and navigates back without asking to discard', async () => {
  const confirm = vi.spyOn(window, 'confirm'); await open();
  expect(document.activeElement).toBe(screen.getByRole('heading', { level: 1 }));
  expect(screen.getByText('profile@example.com')).toBeTruthy(); expect(screen.getByText('Active')).toBeTruthy();
  fireEvent.click(screen.getByRole('button', { name: 'Back to directory' }));
  expect(back).toHaveBeenCalledOnce(); expect(confirm).not.toHaveBeenCalled();
});
it('allows cancelling an unchanged draft and disables saving it', async () => {
  const confirm = vi.spyOn(window, 'confirm'); const user = await edit();
  expect((screen.getByRole('button', { name: 'Save changes' }) as HTMLButtonElement).disabled).toBe(true);
  await user.click(screen.getByRole('button', { name: 'Cancel' }));
  expect(screen.queryByRole('form')).toBeNull(); expect(confirm).not.toHaveBeenCalled();
});
it('keeps a dirty draft when discard is declined and restores stored details when accepted', async () => {
  const confirm = vi.spyOn(window, 'confirm').mockReturnValueOnce(false).mockReturnValueOnce(true);
  const user = await edit(); await user.type(screen.getByLabelText('Job title'), ' changed');
  const event = new Event('beforeunload', { cancelable: true }); window.dispatchEvent(event); expect(event.defaultPrevented).toBe(true);
  await user.click(screen.getByRole('button', { name: 'Cancel' }));
  expect((screen.getByLabelText('Job title') as HTMLInputElement).value).toBe('Engineer changed');
  await user.click(screen.getByRole('button', { name: 'Cancel' }));
  expect(screen.getByText('Engineer')).toBeTruthy(); expect(confirm).toHaveBeenCalledTimes(2);
  const clean = new Event('beforeunload', { cancelable: true }); window.dispatchEvent(clean); expect(clean.defaultPrevented).toBe(false);
});
it('requires confirmation before navigating away from a dirty draft', async () => {
  vi.spyOn(window, 'confirm').mockReturnValueOnce(false).mockReturnValueOnce(true);
  const user = await edit(); await user.type(screen.getByLabelText('Department'), ' changed');
  await user.click(screen.getByRole('button', { name: 'Back to directory' })); expect(back).not.toHaveBeenCalled();
  await user.click(screen.getByRole('button', { name: 'Back to directory' })); expect(back).toHaveBeenCalledOnce();
});
it('saves edited values once, prevents duplicate submission, and adopts server-normalized values', async () => {
  const pending = deferred(); update.mockReturnValue(pending.promise);
  const user = await edit(); await user.clear(screen.getByLabelText('Job title')); await user.type(screen.getByLabelText('Job title'), 'Senior Engineer');
  await user.selectOptions(screen.getByLabelText('Status'), 'ON_LEAVE');
  const form = screen.getByRole('form', { name: 'Edit employee profile' });
  fireEvent.submit(form); fireEvent.submit(form);
  expect(update).toHaveBeenCalledTimes(1);
  expect(update).toHaveBeenCalledWith(7, expect.objectContaining({ jobTitle: 'Senior Engineer', status: 'ON_LEAVE' }));
  expect((screen.getByRole('button', { name: 'Back to directory' }) as HTMLButtonElement).disabled).toBe(true);
  expect(screen.getByRole('button', { name: 'Saving…' })).toBeTruthy();
  const event = new Event('beforeunload', { cancelable: true }); window.dispatchEvent(event); expect(event.defaultPrevented).toBe(true);
  const result = { ...employee, jobTitle: 'Senior Engineer', status: 'ON_LEAVE' as const };
  await act(async () => pending.resolve(result));
  expect(saved).toHaveBeenCalledWith(result); expect(screen.getByText('On leave')).toBeTruthy();
  expect(screen.getByRole('status').textContent).toContain('updated successfully');
  expect(screen.queryByRole('form')).toBeNull();
});
it.each([new Error('Email is already registered'), 'unexpected'])('retains draft and permits retry after save failure: %s', async cause => {
  update.mockRejectedValueOnce(cause).mockResolvedValue({ ...employee, firstName: 'Changed' });
  const user = await edit(); await user.clear(screen.getByLabelText('First name')); await user.type(screen.getByLabelText('First name'), 'Changed');
  await user.click(screen.getByRole('button', { name: 'Save changes' }));
  expect((await screen.findByRole('alert')).textContent).toContain(cause instanceof Error ? cause.message : 'Unable to save employee.');
  expect((screen.getByLabelText('First name') as HTMLInputElement).value).toBe('Changed'); expect(saved).not.toHaveBeenCalled();
  await user.click(screen.getByRole('button', { name: 'Save changes' }));
  await screen.findByText('Employee profile updated successfully.'); expect(saved).toHaveBeenCalledOnce();
});
it.each([new Error('Employee was deleted'), null])('retries loading after a failure: %s', async cause => {
  get.mockRejectedValueOnce(cause).mockResolvedValue(employee);
  const user = userEvent.setup(); render(<EmployeeProfile employeeId={7} onBack={back} onSaved={saved} />);
  expect((await screen.findByRole('alert')).textContent).toContain(cause instanceof Error ? cause.message : 'Unable to load employee.');
  await user.click(screen.getByRole('button', { name: /^Retry$/ }));
  await screen.findByText('Engineer'); expect(screen.queryByRole('alert')).toBeNull();
});
it.each(['resolve', 'reject'] as const)('ignores a stale load that later %ss', async outcome => {
  const old = deferred(), current = deferred(); get.mockReturnValueOnce(old.promise).mockReturnValueOnce(current.promise);
  const { rerender } = render(<EmployeeProfile employeeId={7} onBack={back} onSaved={saved} />);
  expect(screen.getByRole('status').textContent).toContain('Loading'); const signal = get.mock.calls[0][1]!;
  rerender(<EmployeeProfile employeeId={8} onBack={back} onSaved={saved} />); expect(signal.aborted).toBe(true);
  await act(async () => { if (outcome === 'resolve') old.resolve(employee); else old.reject(new Error('stale')); });
  expect(screen.getByRole('status').textContent).toContain('Loading'); expect(screen.queryByRole('alert')).toBeNull();
  await act(async () => current.resolve({ ...employee, id: 8, firstName: 'Current' }));
  expect(screen.getByRole('heading').textContent).toBe('Current Profile'); expect(screen.queryByRole('status')).toBeNull();
});

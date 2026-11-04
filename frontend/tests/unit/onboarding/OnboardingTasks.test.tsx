import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import OnboardingTasks from '../../../src/features/employees/OnboardingTasks';
import { api } from '../../../src/lib/api';
vi.mock('../../../src/lib/api', () => ({ api: vi.fn() }));
const call = vi.mocked(api);
const empty = { items: [], page: 0, totalPages: 0, totalElements: 0, totalTasks: 0, completedTasks: 0 };
const task = { id: 1, title: 'Prepare laptop', owner: 'IT', dueOn: '2026-12-01', completed: false };
const other = { id: 2, title: 'Orientation', owner: 'HR', dueOn: null, completed: true };
const populated = { ...empty, items: [task, other], totalPages: 1, totalElements: 2, totalTasks: 2, completedTasks: 1 };
const button = (name: string) => screen.getByRole('button', { name });
const change = (name: string, value: string) => fireEvent.change(screen.getByLabelText(name), { target: { value } });
function deferred() { let resolve!: (value: unknown) => void; let reject!: (cause: unknown) => void; const promise = new Promise((yes, no) => { resolve = yes; reject = no; }); return { promise, resolve, reject }; }
beforeEach(() => call.mockReset());
afterEach(() => { cleanup(); vi.restoreAllMocks(); });
it('combines completion and deadline filters and clears the deadline independently', async () => {
  call.mockResolvedValue(empty); const user = userEvent.setup(); render(<OnboardingTasks employeeId={7} />);
  await screen.findByText('No onboarding tasks yet.'); change('Tasks due on or before', '2026-12-01'); await screen.findByText('No tasks match these filters.');
  await user.selectOptions(screen.getByLabelText('Show onboarding tasks'), 'false');
  await waitFor(() => expect(call).toHaveBeenLastCalledWith('/api/v1/employees/7/onboarding-tasks?page=0&completed=false&dueBy=2026-12-01', expect.any(Object)));
  expect(screen.getByText(/tasks without a due date are excluded/)).toBeTruthy(); await user.click(button('Clear due-date filter'));
  await waitFor(() => expect(call).toHaveBeenLastCalledWith('/api/v1/employees/7/onboarding-tasks?page=0&completed=false', expect.any(Object)));
  await user.selectOptions(screen.getByLabelText('Show onboarding tasks'), ''); await screen.findByText('No onboarding tasks yet.');
});
it('requires both trimmed fields, sends a dated creation once while pending, and clears the draft', async () => {
  const save = deferred(); call.mockImplementation((_url, options) => options?.method ? save.promise : Promise.resolve(empty));
  render(<OnboardingTasks employeeId={7} />); await screen.findByText('No onboarding tasks yet.'); const form = screen.getByRole('form', { name: 'Add onboarding task' });
  change('Task title', ' '); fireEvent.submit(form); change('Task title', ' Laptop '); change('Owner label', ' '); fireEvent.submit(form); expect(call).toHaveBeenCalledTimes(1);
  change('Owner label', ' IT '); change('Due date (optional)', '2026-12-01'); fireEvent.submit(form); fireEvent.submit(form);
  expect(call.mock.calls.filter(([, o]) => o?.method)).toHaveLength(1);
  expect(call).toHaveBeenLastCalledWith('/api/v1/employees/7/onboarding-tasks', { method: 'POST', body: JSON.stringify({ title: 'Laptop', owner: 'IT', dueOn: '2026-12-01' }) });
  expect((screen.getByLabelText('Task title').closest('fieldset') as HTMLFieldSetElement).disabled).toBe(true);
  await act(async () => save.resolve(task)); await screen.findByText('Task added.'); expect((screen.getByLabelText('Task title') as HTMLInputElement).value).toBe('');
});
it('confirms draft replacement and dirty cancellation, cancels clean dated and undated edits, and clears a saved date', async () => {
  call.mockResolvedValue(populated); const confirm = vi.spyOn(window, 'confirm'); const user = userEvent.setup(); render(<OnboardingTasks employeeId={7} />); await screen.findByText('Prepare laptop');
  change('Task title', 'Draft'); confirm.mockReturnValueOnce(false).mockReturnValueOnce(true);
  await user.click(button('Edit task Prepare laptop')); expect(screen.queryByRole('form', { name: 'Edit onboarding task' })).toBeNull();
  await user.click(button('Edit task Prepare laptop')); await user.click(button('Cancel task edit'));
  await user.click(button('Edit task Orientation')); await user.click(button('Cancel task edit'));
  await user.click(button('Edit task Orientation')); change('Owner label', 'New HR'); confirm.mockReturnValueOnce(false).mockReturnValueOnce(true);
  await user.click(button('Cancel task edit')); expect((screen.getByLabelText('Owner label') as HTMLInputElement).value).toBe('New HR');
  await user.click(button('Cancel task edit')); expect(screen.queryByRole('form', { name: 'Edit onboarding task' })).toBeNull();
  await user.click(button('Edit task Prepare laptop')); change('Due date (optional)', ''); await user.click(button('Save task changes')); await screen.findByText('Task details updated.');
  expect(call).toHaveBeenCalledWith('/api/v1/employees/7/onboarding-tasks/1', { method: 'PUT', body: JSON.stringify({ title: 'Prepare laptop', owner: 'IT', dueOn: null }) });
});
it.each([new Error('Save rejected'), null])('retains failed edits and retries: %s', async cause => {
  let failed = false; call.mockImplementation((_url, options) => { if (!options?.method) return Promise.resolve(populated); if (!failed) { failed = true; return Promise.reject(cause); } return Promise.resolve(task); });
  const user = userEvent.setup(); render(<OnboardingTasks employeeId={7} />); await screen.findByText('Prepare laptop'); await user.click(button('Edit task Prepare laptop')); change('Task title', 'Revised');
  await user.click(button('Save task changes')); expect((await screen.findByRole('alert')).textContent).toContain(cause instanceof Error ? cause.message : 'Unable to save task.');
  expect((screen.getByLabelText('Task title') as HTMLInputElement).value).toBe('Revised'); await user.click(button('Save task changes')); await screen.findByText('Task details updated.');
});
it('completes, reopens and removes only after confirmation', async () => {
  call.mockResolvedValue(populated); const confirm = vi.spyOn(window, 'confirm').mockReturnValueOnce(false).mockReturnValue(true); const user = userEvent.setup(); render(<OnboardingTasks employeeId={7} />); await screen.findByText('Prepare laptop');
  await user.click(button('Complete Prepare laptop')); await screen.findByText('Task updated.');
  expect(call).toHaveBeenCalledWith('/api/v1/employees/7/onboarding-tasks/1/completion', { method: 'PUT', body: JSON.stringify({ completed: true }) });
  await user.click(button('Reopen Orientation')); await screen.findByText('Task updated.');
  expect(call).toHaveBeenCalledWith('/api/v1/employees/7/onboarding-tasks/2/completion', { method: 'PUT', body: JSON.stringify({ completed: false }) });
  await user.click(button('Remove task Prepare laptop')); expect(call.mock.calls.some(([, o]) => o?.method === 'DELETE')).toBe(false);
  await user.click(button('Remove task Prepare laptop')); await screen.findByText('Task removed.'); expect(confirm).toHaveBeenCalledWith('Remove task "Prepare laptop"?');
  expect(call).toHaveBeenCalledWith('/api/v1/employees/7/onboarding-tasks/1', { method: 'DELETE' });
});
it.each([new Error('Load failed'), null])('recovers from load failures: %s', async cause => {
  call.mockRejectedValueOnce(cause).mockResolvedValue(empty); const user = userEvent.setup(); render(<OnboardingTasks employeeId={7} />);
  expect((await screen.findByRole('alert')).textContent).toContain(cause instanceof Error ? cause.message : 'Unable to load checklist.'); await user.click(button('Refresh checklist')); await screen.findByText('No onboarding tasks yet.'); expect(screen.queryByRole('alert')).toBeNull();
});
it.each(['resolve', 'reject'])('ignores abandoned employee requests that %s', async outcome => {
  const old = deferred(); call.mockReturnValueOnce(old.promise).mockResolvedValue(empty); const { rerender } = render(<OnboardingTasks employeeId={7} />); const signal = call.mock.calls[0][1]?.signal as AbortSignal;
  rerender(<OnboardingTasks employeeId={8} />); await screen.findByText('No onboarding tasks yet.'); expect(signal.aborted).toBe(true);
  await act(async () => { if (outcome === 'resolve') old.resolve(populated); else old.reject(new Error('Stale')); }); expect(screen.queryByText('Prepare laptop')).toBeNull(); expect(screen.queryByRole('alert')).toBeNull();
});
it('navigates pages and recovers when the last page disappears', async () => {
  let shrinking = false; call.mockImplementation(async url => ({ ...populated, page: Number(new URL(String(url), 'http://localhost').searchParams.get('page')), totalPages: shrinking ? 1 : 2 }));
  const user = userEvent.setup(); render(<OnboardingTasks employeeId={7} />); await screen.findByText('Prepare laptop'); await user.click(button('Next tasks')); await screen.findByText('Page 2 of 2'); await user.click(button('Previous tasks')); await screen.findByText('Page 1 of 2'); shrinking = true;
  await user.click(button('Next tasks')); await waitFor(() => expect(call).toHaveBeenLastCalledWith('/api/v1/employees/7/onboarding-tasks?page=0', expect.any(Object))); await screen.findByText('Prepare laptop'); expect(screen.queryByRole('navigation')).toBeNull();
});

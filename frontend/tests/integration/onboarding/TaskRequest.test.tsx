import { afterEach, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import OnboardingTasks from '../../../src/features/employees/OnboardingTasks';
afterEach(() => { cleanup(); vi.unstubAllGlobals(); });
it('retains a task draft after HTTP validation rejection and retries through the real client', async () => {
  let valid = false;
  const fetch = vi.fn(async (_url: RequestInfo | URL, options?: RequestInit) => options?.method === 'POST'
    ? new Response(JSON.stringify(valid ? { id: 1 } : { detail: 'Task rejected.' }), { status: valid ? 201 : 400 })
    : new Response(JSON.stringify({ items: [], page: 0, totalPages: 0, totalElements: 0, totalTasks: 0, completedTasks: 0 }), { status: 200 }));
  vi.stubGlobal('fetch', fetch); const user = userEvent.setup(); render(<OnboardingTasks employeeId={7} />); await screen.findByText('No onboarding tasks yet.');
  fireEvent.change(screen.getByLabelText('Task title'), { target: { value: ' Synthetic induction ' } }); fireEvent.change(screen.getByLabelText('Owner label'), { target: { value: ' HR ' } });
  await user.click(screen.getByRole('button', { name: 'Add task' })); expect((await screen.findByRole('alert')).textContent).toContain('Task rejected.');
  expect((screen.getByLabelText('Task title') as HTMLInputElement).value).toBe(' Synthetic induction ');
  expect(fetch).toHaveBeenLastCalledWith('/api/v1/employees/7/onboarding-tasks', expect.objectContaining({ method: 'POST', body: JSON.stringify({ title: 'Synthetic induction', owner: 'HR', dueOn: null }) }));
  valid = true; await user.click(screen.getByRole('button', { name: 'Add task' })); await screen.findByText('Task added.'); expect(screen.queryByRole('alert')).toBeNull();
});

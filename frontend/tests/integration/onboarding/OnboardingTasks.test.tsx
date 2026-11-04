import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import OnboardingTasks from '../../../src/features/employees/OnboardingTasks';

const requests: string[] = [];
beforeEach(() => { requests.length = 0; });
afterEach(() => { cleanup(); vi.unstubAllGlobals(); });

it('integrates the checklist with the HTTP client and preserves global progress while filtering', async () => {
  vi.stubGlobal('fetch', vi.fn(async (input: string) => {
    requests.push(input);
    const filtered = input.includes('completed=true');
    return new Response(JSON.stringify({
      items: filtered ? [] : [{ id: 1, title: 'Laptop', owner: 'IT team', dueOn: null, completed: false }],
      page: 0, totalPages: filtered ? 0 : 1, totalElements: filtered ? 0 : 1, totalTasks: 1, completedTasks: 0,
    }), { status: 200 });
  }));
  const user = userEvent.setup();
  render(<OnboardingTasks employeeId={7} />);
  await screen.findByText('Laptop');
  await user.selectOptions(screen.getByLabelText('Show onboarding tasks'), 'true');
  await screen.findByText('No tasks match these filters.');
  expect(requests.at(-1)).toContain('/api/v1/employees/7/onboarding-tasks?page=0&completed=true');
  expect(screen.getByText('0 of 1 tasks complete · 0 matching tasks')).toBeTruthy();
});

it('retains the task draft after a backend save error', async () => {
  vi.stubGlobal('fetch', vi.fn(async (_input: string, options?: RequestInit) => {
    if (options?.method === 'POST') return new Response('{"detail":"Unable to save"}', { status: 409 });
    return new Response(JSON.stringify({ items: [], page: 0, totalPages: 0, totalElements: 0, totalTasks: 0, completedTasks: 0 }));
  }));
  const user = userEvent.setup();
  render(<OnboardingTasks employeeId={7} />);
  await screen.findByText('No onboarding tasks yet.');
  await user.type(screen.getByLabelText('Task title'), 'Laptop');
  await user.type(screen.getByLabelText('Owner label'), 'IT');
  await user.click(screen.getByRole('button', { name: /^Add task$/ }));
  await waitFor(() => expect(screen.getByRole('alert').textContent).toContain('Unable to save'));
  expect((screen.getByLabelText('Task title') as HTMLInputElement).value).toBe('Laptop');
  expect((screen.getByLabelText('Owner label') as HTMLInputElement).value).toBe('IT');
});

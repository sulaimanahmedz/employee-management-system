import { afterEach, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import EmployeeProfile from '../../../src/features/employees/EmployeeProfile';
afterEach(() => { cleanup(); vi.unstubAllGlobals(); });
it('sends edited fields through the HTTP client and preserves them on a backend conflict', async () => {
  const employee = { id: 7, firstName: 'Synthetic', lastName: 'Profile', email: 'profile@example.com', department: 'IT', jobTitle: 'Engineer', status: 'ACTIVE' };
  const requests: { url: string; options?: RequestInit }[] = [];
  vi.stubGlobal('fetch', vi.fn(async (url: string, options?: RequestInit) => {
    requests.push({ url, options });
    if (options?.method === 'PUT') return new Response('{"detail":"Email already registered"}', { status: 409 });
    return new Response(JSON.stringify(url === '/api/v1/employees/7' ? employee : { items: [], page: 0, size: 10, totalElements: 0, totalPages: 0, totalTasks: 0, completedTasks: 0 }));
  }));
  const saved = vi.fn(), user = userEvent.setup(); render(<EmployeeProfile employeeId={7} onBack={vi.fn()} onSaved={saved} />);
  await screen.findByText('Engineer'); await user.click(screen.getByRole('button', { name: 'Edit profile' }));
  await user.clear(screen.getByLabelText('Work email')); await user.type(screen.getByLabelText('Work email'), 'conflict@example.com');
  await user.click(screen.getByRole('button', { name: 'Save changes' }));
  expect((await screen.findByRole('alert')).textContent).toContain('Email already registered');
  expect((screen.getByLabelText('Work email') as HTMLInputElement).value).toBe('conflict@example.com');
  const put = requests.find(r => r.options?.method === 'PUT')!;
  expect(put.url).toBe('/api/v1/employees/7');
  expect(JSON.parse(put.options!.body as string)).toEqual({ firstName: 'Synthetic', lastName: 'Profile', email: 'conflict@example.com', department: 'IT', jobTitle: 'Engineer', status: 'ACTIVE' });
  expect(saved).not.toHaveBeenCalled();
});

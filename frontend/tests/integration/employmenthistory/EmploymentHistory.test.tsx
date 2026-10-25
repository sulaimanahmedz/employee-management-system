import { afterEach, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import EmploymentHistory from '../../../src/features/employees/EmploymentHistory';
afterEach(() => { cleanup(); vi.unstubAllGlobals(); });
it('integrates event filtering with the real API helper and HTTP error recovery', async () => {
  const requests: string[] = [];
  let fail = true;
  vi.stubGlobal('fetch', vi.fn(async (url: string) => {
    requests.push(url);
    if (fail) { fail = false; return new Response('{"detail":"History service unavailable"}', { status: 503 }); }
    return new Response(JSON.stringify({ items: [], page: 0, size: 10, totalPages: 0, totalElements: 0 }));
  }));
  const user = userEvent.setup(); render(<EmploymentHistory employeeId={42} />);
  expect((await screen.findByRole('alert')).textContent).toContain('History service unavailable');
  await user.click(screen.getByRole('button', { name: 'Retry history' }));
  await screen.findByText(/No history recorded yet/);
  await user.selectOptions(screen.getByLabelText('Show history events'), 'CHANGED');
  await screen.findByText('No history events match this type.');
  expect(requests).toEqual([
    '/api/v1/employees/42/history?page=0&size=10',
    '/api/v1/employees/42/history?page=0&size=10',
    '/api/v1/employees/42/history?page=0&size=10&kind=CHANGED',
  ]);
});

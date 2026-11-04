import { afterEach, expect, it, vi } from 'vitest';
import { act, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createRoot } from 'react-dom/client';
// Observe the root for cleanup while keeping the real renderer and application.
vi.mock('react-dom/client', async importOriginal => {
  const original = await importOriginal<typeof import('react-dom/client')>();
  return { ...original, createRoot: vi.fn(original.createRoot) };
});
afterEach(async () => {
  for (const result of vi.mocked(createRoot).mock.results) {
    if (result.type === 'return') await act(async () => result.value.unmount());
  }
  document.body.replaceChildren(); vi.unstubAllGlobals();
});
it('boots the real application in StrictMode and recovers from a directory HTTP failure', async () => {
  const root = document.createElement('div'); root.id = 'root'; document.body.append(root);
  let failing = true;
  const fetch = vi.fn(async () => failing
    ? new Response(JSON.stringify({ detail: 'Directory temporarily unavailable' }), { status: 503 })
    : new Response(JSON.stringify({ items: [], page: 0, size: 10, totalPages: 0, totalElements: 0, totalEmployees: 0, activeEmployees: 0, departments: 0 }), { status: 200 }));
  vi.stubGlobal('fetch', fetch);
  await act(async () => { await import('../../../src/main'); });
  expect(createRoot).toHaveBeenCalledWith(root);
  expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('Your people, connected.');
  expect((await screen.findByRole('alert')).textContent).toContain('Directory temporarily unavailable');
  failing = false;
  await userEvent.setup().click(screen.getByRole('button', { name: 'Retry directory' }));
  await screen.findByText('Your directory starts here');
  expect(screen.queryByRole('alert')).toBeNull();
  expect(screen.getByRole('link', { name: 'Employee directory' }).getAttribute('href')).toBe('#directory');
  expect(fetch).toHaveBeenCalledWith(expect.stringContaining('/api/v1/employees/directory?'), expect.objectContaining({ signal: expect.any(AbortSignal) }));
});

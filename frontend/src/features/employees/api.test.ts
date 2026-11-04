import { afterEach, expect, it, vi } from 'vitest';
import { listEmployees, createEmployee, getEmployee, updateEmployee, searchEmployees, getEmploymentHistory } from './api';
import type { EmployeeInput } from './types';

afterEach(() => vi.unstubAllGlobals());
const employee: EmployeeInput = { firstName: 'Synthetic', lastName: 'Person', email: 'test@example.com', department: 'IT', jobTitle: 'Engineer', status: 'ACTIVE' };

it('uses employee-scoped routes and preserves input and cancellation signals', async () => {
  const fetcher = vi.fn().mockImplementation(async () => new Response(JSON.stringify(employee)));
  vi.stubGlobal('fetch', fetcher);
  const signal = new AbortController().signal;
  await listEmployees(signal);
  expect(fetcher).toHaveBeenLastCalledWith('/api/v1/employees', expect.objectContaining({ signal }));
  await getEmployee(42, signal);
  expect(fetcher).toHaveBeenLastCalledWith('/api/v1/employees/42', expect.objectContaining({ signal }));
  expect(await createEmployee(employee)).toEqual(employee);
  expect(fetcher).toHaveBeenLastCalledWith('/api/v1/employees', expect.objectContaining({ method: 'POST', body: JSON.stringify(employee) }));
  await updateEmployee(42, employee);
  expect(fetcher).toHaveBeenLastCalledWith('/api/v1/employees/42', expect.objectContaining({ method: 'PUT', body: JSON.stringify(employee) }));
});

it('encodes search text, pagination and sort without accidentally injecting filters', async () => {
  const fetcher = vi.fn().mockImplementation(async () => new Response('{"items":[]}'));
  vi.stubGlobal('fetch', fetcher);
  for (const status of ['', 'ACTIVE'] as const) {
    const signal = new AbortController().signal;
    await searchEmployees({ q: 'IT & HR+50%', status, size: 20, sortBy: 'name', direction: 'desc' }, 2, signal);
    const [path, options] = fetcher.mock.calls.at(-1)!;
    const params = new URL(path, 'http://localhost').searchParams;
    expect(params.get('q')).toBe('IT & HR+50%');
    expect(params.get('page')).toBe('2');
    expect(params.get('size')).toBe('20');
    expect(params.get('sortBy')).toBe('name');
    expect(params.get('direction')).toBe('desc');
    expect(params.get('status')).toBe(status || null);
    expect(options.signal).toBe(signal);
  }
});

it('supports unfiltered and kind-filtered employment history', async () => {
  const fetcher = vi.fn().mockImplementation(async () => new Response('{"items":[]}'));
  vi.stubGlobal('fetch', fetcher);
  await getEmploymentHistory(7, 0);
  expect(fetcher.mock.calls.at(-1)![0]).toBe('/api/v1/employees/7/history?page=0&size=10');
  const signal = new AbortController().signal;
  await getEmploymentHistory(7, 2, signal, 'CHANGED');
  expect(fetcher).toHaveBeenLastCalledWith('/api/v1/employees/7/history?page=2&size=10&kind=CHANGED', expect.objectContaining({ signal }));
});

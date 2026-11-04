import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { act, cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import EmploymentHistory from '../../../src/features/employees/EmploymentHistory';
import { getEmploymentHistory } from '../../../src/features/employees/api';
import type { EmploymentHistoryPage } from '../../../src/features/employees/types';
vi.mock('../../../src/features/employees/api', () => ({ getEmploymentHistory: vi.fn() }));
const load = vi.mocked(getEmploymentHistory);
const empty: EmploymentHistoryPage = { items: [], page: 0, size: 10, totalElements: 0, totalPages: 0 };
const record = (id: number, kind: 'CREATED' | 'BASELINE' | 'CHANGED', status: 'ACTIVE' | 'ON_LEAVE' | 'INACTIVE') => ({
  id, kind, status, recordedAt: '2026-10-25T08:00:00Z', department: 'Synthetic IT', jobTitle: `Role ${id}`,
});
function deferred() {
  let resolve!: (value: EmploymentHistoryPage) => void;
  let reject!: (reason: unknown) => void;
  const promise = new Promise<EmploymentHistoryPage>((yes, no) => { resolve = yes; reject = no; });
  return { promise, resolve, reject };
}
beforeEach(() => load.mockReset());
afterEach(cleanup);

it('shows initial loading, an unfiltered empty state, then a filtered empty state', async () => {
  const pending = deferred(); load.mockReturnValueOnce(pending.promise).mockResolvedValue(empty);
  const user = userEvent.setup(); render(<EmploymentHistory employeeId={7} />);
  expect(screen.getByRole('status').textContent).toContain('Loading');
  await act(async () => pending.resolve(empty));
  expect(screen.getByText(/No history recorded yet/)).toBeTruthy();
  await user.selectOptions(screen.getByLabelText('Show history events'), 'BASELINE');
  await screen.findByText('No history events match this type.');
  expect(load).toHaveBeenLastCalledWith(7, 0, expect.any(AbortSignal), 'BASELINE');
  expect(screen.queryByRole('navigation')).toBeNull();
});

it('renders event and status labels, navigates both ways and resets page on filter changes', async () => {
  load.mockImplementation(async (_id, page) => ({ ...empty, page, totalElements: 3, totalPages: 2,
    items: page === 0 ? [record(1, 'CREATED', 'ACTIVE'), record(2, 'BASELINE', 'ON_LEAVE')] : [record(3, 'CHANGED', 'INACTIVE')] }));
  const user = userEvent.setup(); const { container } = render(<EmploymentHistory employeeId={7} />);
  await screen.findByText('Synthetic IT · Role 1 · Active');
  expect(screen.getByText('Synthetic IT · Role 2 · On leave')).toBeTruthy();
  expect(container.querySelector('time')?.dateTime).toBe('2026-10-25T08:00:00Z');
  expect((screen.getByRole('button', { name: 'Previous history' }) as HTMLButtonElement).disabled).toBe(true);
  await user.click(screen.getByRole('button', { name: 'Next history' }));
  await screen.findByText('Synthetic IT · Role 3 · Inactive');
  expect((screen.getByRole('button', { name: 'Next history' }) as HTMLButtonElement).disabled).toBe(true);
  await user.click(screen.getByRole('button', { name: 'Previous history' }));
  await screen.findByText('Synthetic IT · Role 1 · Active');
  await user.click(screen.getByRole('button', { name: 'Next history' }));
  await screen.findByText('Synthetic IT · Role 3 · Inactive');
  await user.selectOptions(screen.getByLabelText('Show history events'), 'CHANGED');
  await screen.findByText('Synthetic IT · Role 1 · Active');
  expect(load).toHaveBeenLastCalledWith(7, 0, expect.any(AbortSignal), 'CHANGED');
});

it.each([new Error('History unavailable'), 'unexpected rejection'])('recovers from a failed request by retrying: %s', async cause => {
  load.mockRejectedValueOnce(cause).mockResolvedValue(empty);
  const user = userEvent.setup(); render(<EmploymentHistory employeeId={7} />);
  const alert = await screen.findByRole('alert');
  expect(alert.textContent).toContain(cause instanceof Error ? cause.message : 'Unable to load history.');
  expect(screen.queryByRole('status')).toBeNull();
  await user.click(screen.getByRole('button', { name: 'Retry history' }));
  await screen.findByText(/No history recorded yet/);
  expect(screen.queryByRole('alert')).toBeNull(); expect(load).toHaveBeenCalledTimes(2);
});

it.each(['resolve', 'reject'] as const)('ignores an abandoned employee request that later %ss', async outcome => {
  const old = deferred(), current = deferred();
  load.mockReturnValueOnce(old.promise).mockReturnValueOnce(current.promise);
  const { rerender } = render(<EmploymentHistory employeeId={7} />);
  const signal = load.mock.calls[0][2]!;
  rerender(<EmploymentHistory employeeId={8} />); expect(signal.aborted).toBe(true);
  await act(async () => { if (outcome === 'resolve') old.resolve({ ...empty, totalElements: 99 }); else old.reject(new Error('stale error')); });
  expect(screen.getByRole('status')).toBeTruthy(); expect(screen.queryByRole('alert')).toBeNull();
  expect(screen.queryByText('99 matching history events')).toBeNull();
  await act(async () => current.resolve(empty));
  await waitFor(() => expect(screen.queryByRole('status')).toBeNull());
  expect(screen.getByText('0 matching history events')).toBeTruthy();
});

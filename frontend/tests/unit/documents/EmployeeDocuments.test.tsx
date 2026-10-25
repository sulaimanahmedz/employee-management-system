import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import EmployeeDocuments from '../../../src/features/employees/EmployeeDocuments';
import { api } from '../../../src/lib/api';
vi.mock('../../../src/lib/api', () => ({ api: vi.fn() }));
const call = vi.mocked(api);
const empty = { items: [], page: 0, totalPages: 0, totalElements: 0 };
const record = { id: 1, name: 'Contract A', category: 'CONTRACT', expiresOn: '2026-12-31' };
const undated = { id: 2, name: 'Certificate B', category: 'CERTIFICATE', expiresOn: null };
const populated = { ...empty, items: [record, undated], totalPages: 1, totalElements: 2 };
const button = (name: string) => screen.getByRole('button', { name });
const change = (name: string, value: string) => fireEvent.change(screen.getByLabelText(name), { target: { value } });
function deferred() { let resolve!: (value: unknown) => void; let reject!: (cause: unknown) => void; const promise = new Promise((yes, no) => { resolve = yes; reject = no; }); return { promise, resolve, reject }; }
beforeEach(() => call.mockReset());
afterEach(cleanup);
it('applies trimmed search, category and expiry, then clears filters', async () => {
  call.mockResolvedValue(empty); const user = userEvent.setup(); render(<EmployeeDocuments employeeId={7} />);
  await screen.findByText('No document metadata registered.'); change('Search document names', '  Contract & A  ');
  await user.selectOptions(screen.getByLabelText('Document category'), 'CONTRACT'); change('Expires on or before', '2026-12-31');
  await user.click(button('Apply document filters')); await screen.findByText('No document metadata matches these filters.');
  expect(call).toHaveBeenLastCalledWith('/api/v1/employees/7/documents/page?page=0&q=Contract%20%26%20A&expiresBy=2026-12-31&category=CONTRACT', expect.any(Object));
  expect(screen.getByText(/Showing category: Contract/)).toBeTruthy(); expect(screen.getByText(/Showing expiry dates on or before 2026-12-31/)).toBeTruthy();
  await user.click(button('Clear document filters')); await screen.findByText('No document metadata registered.');
  expect(call).toHaveBeenLastCalledWith('/api/v1/employees/7/documents/page?page=0&q=', expect.any(Object)); expect((screen.getByLabelText('Search document names') as HTMLInputElement).value).toBe('');
});
it('rejects blank names, trims creation data and prevents duplicate submissions while pending', async () => {
  const save = deferred(); call.mockImplementation((_url, options) => options?.method ? save.promise : Promise.resolve(empty));
  const user = userEvent.setup(); render(<EmployeeDocuments employeeId={7} />); await screen.findByText('0 matching document records');
  const form = screen.getByRole('form', { name: 'Add document metadata' }); change('Document name', '   '); fireEvent.submit(form); expect(call).toHaveBeenCalledTimes(1);
  change('Document name', '  Safety certificate  '); await user.selectOptions(screen.getByLabelText('Category', { exact: true }), 'CERTIFICATE'); change('Expiry date (optional)', '2026-12-31');
  fireEvent.submit(form); fireEvent.submit(form); expect(call.mock.calls.filter(([, o]) => o?.method)).toHaveLength(1);
  expect(call).toHaveBeenLastCalledWith('/api/v1/employees/7/documents', { method: 'POST', body: JSON.stringify({ name: 'Safety certificate', category: 'CERTIFICATE', expiresOn: '2026-12-31' }) });
  expect((screen.getByLabelText('Document name').closest('fieldset') as HTMLFieldSetElement).disabled).toBe(true);
  await act(async () => save.resolve(record)); await screen.findByText('Document metadata added.'); expect((screen.getByLabelText('Document name') as HTMLInputElement).value).toBe('');
});
it('confirms discarding drafts, edits dated and undated metadata, and cancels clean or dirty edits', async () => {
  call.mockResolvedValue(populated); const confirm = vi.spyOn(window, 'confirm'); const user = userEvent.setup(); render(<EmployeeDocuments employeeId={7} />);
  await screen.findByText('Contract A'); expect(screen.getByText('No expiry date', { exact: false })).toBeTruthy();
  change('Document name', 'Draft'); confirm.mockReturnValueOnce(false).mockReturnValueOnce(true);
  await user.click(button('Edit Contract A')); expect(screen.queryByRole('form', { name: 'Edit document metadata' })).toBeNull();
  await user.click(button('Edit Contract A')); await user.click(button('Cancel metadata edit'));
  await user.click(button('Edit Certificate B')); await user.click(button('Cancel metadata edit'));
  await user.click(button('Edit Certificate B')); change('Document name', 'Revised certificate'); confirm.mockReturnValueOnce(false).mockReturnValueOnce(true);
  await user.click(button('Cancel metadata edit')); expect((screen.getByLabelText('Document name') as HTMLInputElement).value).toBe('Revised certificate');
  await user.click(button('Cancel metadata edit')); expect(screen.queryByRole('form', { name: 'Edit document metadata' })).toBeNull();
  await user.click(button('Edit Contract A')); change('Expiry date (optional)', ''); await user.selectOptions(screen.getByLabelText('Category', { exact: true }), 'OTHER');
  await user.click(button('Save metadata changes')); await screen.findByText('Document metadata updated.');
  expect(call).toHaveBeenCalledWith('/api/v1/employees/7/documents/1', { method: 'PUT', body: JSON.stringify({ name: 'Contract A', category: 'OTHER', expiresOn: null }) });
});
it.each([new Error('Save rejected'), null])('retains failed edits and supports retry: %s', async cause => {
  let failed = false; call.mockImplementation((_url, options) => { if (!options?.method) return Promise.resolve(populated); if (!failed) { failed = true; return Promise.reject(cause); } return Promise.resolve(record); });
  const user = userEvent.setup(); render(<EmployeeDocuments employeeId={7} />); await screen.findByText('Contract A'); await user.click(button('Edit Contract A')); change('Document name', 'Corrected');
  await user.click(button('Save metadata changes')); expect((await screen.findByRole('alert')).textContent).toContain(cause instanceof Error ? cause.message : 'Unable to save document.');
  expect((screen.getByLabelText('Document name') as HTMLInputElement).value).toBe('Corrected'); await user.click(button('Save metadata changes')); await screen.findByText('Document metadata updated.');
});
it.each([new Error('Remove rejected'), undefined])('confirms removal, reports failure and retries: %s', async cause => {
  let failed = false; call.mockImplementation((_url, options) => { if (!options?.method) return Promise.resolve(populated); if (!failed) { failed = true; return Promise.reject(cause); } return Promise.resolve(undefined); });
  const confirm = vi.spyOn(window, 'confirm').mockReturnValueOnce(false).mockReturnValue(true); const user = userEvent.setup(); render(<EmployeeDocuments employeeId={7} />); await screen.findByText('Contract A');
  await user.click(button('Remove Contract A')); expect(call).toHaveBeenCalledTimes(1); await user.click(button('Remove Contract A'));
  expect((await screen.findByRole('alert')).textContent).toContain(cause instanceof Error ? cause.message : 'Unable to remove document.'); expect(screen.getByText('Contract A')).toBeTruthy();
  await user.click(button('Remove Contract A')); await screen.findByText('Document metadata removed.'); expect(confirm).toHaveBeenCalledWith('Remove metadata for "Contract A"?');
  expect(call).toHaveBeenCalledWith('/api/v1/employees/7/documents/1', { method: 'DELETE' });
});
it.each([new Error('Load failed'), null])('recovers from load failures with refresh: %s', async cause => {
  call.mockRejectedValueOnce(cause).mockResolvedValue(empty); const user = userEvent.setup(); render(<EmployeeDocuments employeeId={7} />);
  expect((await screen.findByRole('alert')).textContent).toContain(cause instanceof Error ? cause.message : 'Unable to load documents.');
  await user.click(button('Refresh list')); await screen.findByText('0 matching document records'); expect(screen.queryByRole('alert')).toBeNull();
});
it.each(['resolve', 'reject'])('ignores abandoned employee loads that %s', async outcome => {
  const old = deferred(); call.mockReturnValueOnce(old.promise).mockResolvedValue(empty); const { rerender } = render(<EmployeeDocuments employeeId={7} />);
  const signal = call.mock.calls[0][1]?.signal as AbortSignal; rerender(<EmployeeDocuments employeeId={8} />); await screen.findByText('0 matching document records'); expect(signal.aborted).toBe(true);
  await act(async () => { if (outcome === 'resolve') old.resolve(populated); else old.reject(new Error('Stale')); }); expect(screen.queryByText('Contract A')).toBeNull(); expect(screen.queryByRole('alert')).toBeNull();
});
it('navigates pages and recovers after the last page disappears', async () => {
  let shrinking = false; call.mockImplementation(async url => ({ ...populated, page: Number(new URL(String(url), 'http://localhost').searchParams.get('page')), totalPages: shrinking ? 1 : 2 }));
  const user = userEvent.setup(); render(<EmployeeDocuments employeeId={7} />); await screen.findByText('Contract A');
  await user.click(button('Next documents')); await screen.findByText('Page 2 of 2'); await user.click(button('Previous documents')); await screen.findByText('Page 1 of 2'); shrinking = true;
  await user.click(button('Next documents')); await waitFor(() => expect(call).toHaveBeenLastCalledWith('/api/v1/employees/7/documents/page?page=0&q=', expect.any(Object))); await screen.findByText('Contract A'); expect(screen.queryByRole('navigation')).toBeNull();
});

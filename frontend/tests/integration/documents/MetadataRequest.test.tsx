import { afterEach, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import EmployeeDocuments from '../../../src/features/employees/EmployeeDocuments';
afterEach(() => { cleanup(); vi.unstubAllGlobals(); });
it('maps an HTTP validation failure and retries the retained metadata through the real client', async () => {
  let valid = false;
  const fetch = vi.fn(async (_url: RequestInfo | URL, options?: RequestInit) => options?.method === 'POST'
    ? new Response(JSON.stringify(valid ? { id: 1, name: 'Synthetic', category: 'CONTRACT', expiresOn: null } : { detail: 'Document metadata rejected.' }), { status: valid ? 201 : 400 })
    : new Response(JSON.stringify({ items: [], page: 0, totalPages: 0, totalElements: 0 }), { status: 200 }));
  vi.stubGlobal('fetch', fetch); const user = userEvent.setup(); render(<EmployeeDocuments employeeId={7} />); await screen.findByText('No document metadata registered.');
  fireEvent.change(screen.getByLabelText('Document name'), { target: { value: ' Synthetic ' } });
  await user.click(screen.getByRole('button', { name: 'Add metadata' })); expect((await screen.findByRole('alert')).textContent).toContain('Document metadata rejected.');
  expect((screen.getByLabelText('Document name') as HTMLInputElement).value).toBe(' Synthetic ');
  expect(fetch).toHaveBeenLastCalledWith('/api/v1/employees/7/documents', expect.objectContaining({ method: 'POST', body: JSON.stringify({ name: 'Synthetic', category: 'CONTRACT', expiresOn: null }) }));
  valid = true; await user.click(screen.getByRole('button', { name: 'Add metadata' })); await screen.findByText('Document metadata added.'); expect(screen.queryByRole('alert')).toBeNull();
});

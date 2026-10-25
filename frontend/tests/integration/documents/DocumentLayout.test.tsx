import { afterEach, expect, it, vi } from 'vitest';
import { cleanup, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import EmployeeDocuments from '../../../src/features/employees/EmployeeDocuments';

afterEach(() => { cleanup(); vi.unstubAllGlobals(); });

it('groups document actions below the name and preserves edit behavior', async () => {
  vi.stubGlobal('fetch', vi.fn(async () => new Response(JSON.stringify({
    items: [{ id: 1, name: 'Employment contract', category: 'CONTRACT', expiresOn: null }],
    totalPages: 1, totalElements: 1, page: 0,
  }))));
  render(<EmployeeDocuments employeeId={7} />);
  const title = await screen.findByText('Employment contract', { exact: true });
  const row = title.closest('li')!;
  const actions = row.querySelector('.document-actions')!;
  expect(title.classList.contains('document-name')).toBe(true);
  expect(title.compareDocumentPosition(actions) & Node.DOCUMENT_POSITION_FOLLOWING).not.toBe(0);
  expect(within(actions as HTMLElement).getAllByRole('button')).toHaveLength(2);
  await userEvent.click(screen.getByRole('button', { name: 'Edit Employment contract' }));
  expect((screen.getByLabelText('Document name', { exact: true }) as HTMLInputElement).value).toBe('Employment contract');
});

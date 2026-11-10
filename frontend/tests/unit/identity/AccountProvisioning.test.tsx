import { afterEach, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AccountProvisioning from '../../../src/features/identity/AccountProvisioning';
import { api } from '../../../src/lib/api';
vi.mock('../../../src/lib/api', () => ({ api: vi.fn() }));
afterEach(() => { cleanup(); vi.resetAllMocks(); });
async function fill() {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText('Account login'),'synthetic-new');
  await user.type(screen.getByLabelText('Initial password'),'Synthetic secure password!');
  return user;
}
it('submits the selected role, disables pending controls and resets to least privilege', async () => {
  let resolve!: (value: unknown) => void;
  vi.mocked(api).mockReturnValue(new Promise(yes => {resolve=yes;}));
  render(<AccountProvisioning />); const user=await fill();
  expect((screen.getByLabelText('Account role') as HTMLSelectElement).value).toBe('EMPLOYEE');
  await user.selectOptions(screen.getByLabelText('Account role'),'HR');
  const form=screen.getByRole('form',{name:'Create account'});
  act(() => {fireEvent.submit(form); fireEvent.submit(form);});
  expect(api).toHaveBeenCalledTimes(1);
  expect(api).toHaveBeenCalledWith('/api/v1/admin/accounts',{method:'POST',body:JSON.stringify({login:'synthetic-new',password:'Synthetic secure password!',role:'HR'})});
  expect(screen.getByRole('button',{name:'Creating account…'}).closest('fieldset')?.disabled).toBe(true);
  await act(async () => resolve({id:1,login:'synthetic-new',role:'HR'}));
  expect(screen.getByRole('status').textContent).toContain('Created synthetic-new with role HR');
  expect((screen.getByLabelText('Initial password') as HTMLInputElement).value).toBe('');
  expect((screen.getByLabelText('Account login') as HTMLInputElement).value).toBe('');
  expect((screen.getByLabelText('Account role') as HTMLSelectElement).value).toBe('EMPLOYEE');
});
it.each([new Error('Account login is unavailable'), null])('retains non-secret input after failure and permits retry: %s', async failure => {
  vi.mocked(api).mockRejectedValueOnce(failure).mockResolvedValueOnce({id:2,login:'synthetic-new',role:'ADMIN'});
  render(<AccountProvisioning />); const user=await fill();
  await user.selectOptions(screen.getByLabelText('Account role'),'ADMIN');
  await user.click(screen.getByRole('button',{name:'Create account'}));
  expect((await screen.findByRole('alert')).textContent).toContain(failure instanceof Error ? failure.message : 'Unable to create account');
  expect((screen.getByLabelText('Initial password') as HTMLInputElement).value).toBe('');
  expect((screen.getByLabelText('Account login') as HTMLInputElement).value).toBe('synthetic-new');
  await user.type(screen.getByLabelText('Initial password'),'Synthetic secure password!');
  await user.click(screen.getByRole('button',{name:'Create account'}));
  await screen.findByRole('status'); expect(screen.queryByRole('alert')).toBeNull();
});

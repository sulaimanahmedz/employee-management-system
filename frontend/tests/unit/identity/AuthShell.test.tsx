import { afterEach, expect, it, vi } from 'vitest';
import { act, cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AuthShell from '../../../src/features/identity/AuthShell';
import { setCsrfToken } from '../../../src/lib/api';
vi.mock('../../../src/App', () => ({ default: () => <h1>Employee workspace</h1> }));
afterEach(() => { cleanup(); setCsrfToken(undefined); vi.unstubAllGlobals(); });
function server() {
  const state = { role: '', loginStatus: 204, sessionStatus: 200, logoutStatus: 204, offline: false, csrfOffline: false };
  const fetcher = vi.fn(async (url: string, options?: RequestInit) => {
    if (state.offline) throw new Error('offline');
    if (url.endsWith('/csrf')) { if (state.csrfOffline) throw new Error('offline'); return Response.json({ headerName: 'X-CSRF-TOKEN', token: 'synthetic-token' }); }
    if (url.endsWith('/session')) return state.role ? Response.json({ login: 'synthetic', authorities: [state.role] }, {status: state.sessionStatus}) : new Response(null, { status: 401 });
    if (url.endsWith('/login')) { if (state.loginStatus === 204) state.role = 'ROLE_HR'; return new Response(null, {status: state.loginStatus}); }
    if (url.endsWith('/logout')) return new Response(null, {status: state.logoutStatus});
    throw new Error(`Unexpected test route ${url} ${options?.method}`);
  });
  vi.stubGlobal('fetch', fetcher); return { state, fetcher };
}
it('signs in with CSRF, clears the password after rejection and logs out', async () => {
  const { state, fetcher } = server(); const user = userEvent.setup(); render(<AuthShell />);
  expect(screen.getByText('Checking your session…')).toBeTruthy();
  await screen.findByRole('heading', {name:'Sign in'});
  await user.type(screen.getByLabelText('Login'), 'synthetic');
  await user.type(screen.getByLabelText('Password'), 'invalid-password'); state.loginStatus = 401;
  await user.click(screen.getByRole('button', {name:'Sign in'}));
  expect((await screen.findByRole('alert')).textContent).toContain('wait a minute before retrying');
  expect((screen.getByLabelText('Password') as HTMLInputElement).value).toBe('');
  state.loginStatus = 204; await user.type(screen.getByLabelText('Password'), 'valid-synthetic-password');
  await user.click(screen.getByRole('button', {name:'Sign in'}));
  await screen.findByRole('heading', {name:'Employee workspace'});
  expect(fetcher).toHaveBeenCalledWith('/api/v1/auth/login', expect.objectContaining({headers: expect.objectContaining({'X-CSRF-TOKEN':'synthetic-token'}), body: expect.any(URLSearchParams)}));
  await user.click(screen.getByRole('button', {name:'Sign out'}));
  await screen.findByRole('heading', {name:'Sign in'});
});
it('denies non-HR users and handles session expiry', async () => {
  const {state} = server(); state.role='ROLE_EMPLOYEE'; render(<AuthShell />);
  await screen.findByRole('heading', {name:'Access denied'});
  act(() => window.dispatchEvent(new Event('session-expired')));
  expect(screen.getByRole('alert').textContent).toContain('expired');
  expect(screen.getByRole('heading', {name:'Sign in'})).toBeTruthy();
});
it('recovers from session HTTP errors and network errors', async () => {
  const {state} = server(); state.role='ROLE_HR'; state.sessionStatus=503; render(<AuthShell />);
  expect((await screen.findByRole('alert')).textContent).toContain('Cannot check');
  state.offline=true; await userEvent.click(screen.getByRole('button',{name:'Retry session'}));
  expect((await screen.findByRole('alert')).textContent).toContain('Cannot check');
  state.offline=false; state.sessionStatus=200;
  await userEvent.click(screen.getByRole('button',{name:'Retry session'}));
  await screen.findByRole('heading', {name:'Employee workspace'});
});
it('retains the signed-in screen when logout fails and permits retry', async () => {
  const published = vi.spyOn(Storage.prototype, 'setItem');
  const {state} = server(); state.role='ROLE_HR'; state.logoutStatus=500; render(<AuthShell />);
  await screen.findByRole('heading', {name:'Employee workspace'});
  await userEvent.click(screen.getByRole('button',{name:'Sign out'}));
  expect((await screen.findByRole('alert')).textContent).toContain('Sign out failed');
  expect(published).not.toHaveBeenCalled();
  state.logoutStatus=204; await userEvent.click(screen.getByRole('button',{name:'Sign out'}));
  await screen.findByRole('heading', {name:'Sign in'});
});
it('reports failed login transport and allows another attempt', async () => {
  const {state} = server(); render(<AuthShell />); await screen.findByRole('heading',{name:'Sign in'});
  await userEvent.type(screen.getByLabelText('Login'),'synthetic'); await userEvent.type(screen.getByLabelText('Password'),'synthetic-password');
  state.csrfOffline=true; await userEvent.click(screen.getByRole('button',{name:'Sign in'}));
  expect((await screen.findByRole('alert')).textContent).toContain('Cannot reach');
  expect((screen.getByLabelText('Password') as HTMLInputElement).value).toBe('');
});

it.each(['success', 'failure'])('ignores a late session-check %s after another tab signs out', async (outcome) => {
  let resolve!: (value: Response) => void;
  let reject!: (reason: Error) => void;
  const pending = new Promise<Response>((yes,no) => { resolve=yes; reject=no; });
  const requested = vi.fn();
  vi.stubGlobal('fetch', vi.fn(async (url: string) => {
    if (url.endsWith('/csrf')) return Response.json({headerName:'X-CSRF-TOKEN',token:'synthetic'});
    requested(); return pending;
  }));
  render(<AuthShell />);
  await vi.waitFor(() => expect(requested).toHaveBeenCalled());
  act(() => window.dispatchEvent(new StorageEvent('storage',{key:'ems-sign-out',newValue:'marker'})));
  await screen.findByRole('heading',{name:'Sign in'});
  await act(async () => {
    if (outcome === 'success') resolve(Response.json({login:'synthetic',authorities:['ROLE_HR']}));
    else reject(new Error('Late network failure'));
    await pending.catch(() => undefined);
  });
  expect(screen.queryByRole('heading',{name:'Employee workspace'})).toBeNull();
  expect(screen.getByRole('alert').textContent).toContain('session has expired');
});

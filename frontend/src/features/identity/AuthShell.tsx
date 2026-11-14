import { useEffect, useRef, useState, type FormEvent } from 'react';
import App from '../../App';
import AccountProfile from './AccountProfile';
import AccountProvisioning from './AccountProvisioning';
import { api, setCsrfToken } from '../../lib/api';
import { announceSignOut, listenForSignOut } from './sessionEvents';

type Identity = { login: string; authorities: string[] };
export default function AuthShell() {
  const sessionVersion = useRef(0);
  const [identity, setIdentity] = useState<Identity | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [login, setLogin] = useState('');
  const [password, setPassword] = useState('');

  async function refreshCsrf() {
    const token = await api<{ headerName: string; token: string }>('/api/v1/auth/csrf');
    setCsrfToken(token);
    return token;
  }
  async function loadSession() {
    const version = sessionVersion.current;
    setLoading(true); setError('');
    try {
      await refreshCsrf();
      const response = await fetch('/api/v1/auth/session');
      if (!response.ok && response.status !== 401) throw new Error('Session unavailable');
      const nextIdentity = response.status === 401 ? null : await response.json();
      if (version === sessionVersion.current) setIdentity(nextIdentity);
    } catch { if (version === sessionVersion.current) setError('Cannot check your session. Please retry.'); }
    finally { setLoading(false); }
  }
  useEffect(() => {
    void loadSession();
    const expire = () => { sessionVersion.current++; setLoading(false); setIdentity(null); setPassword(''); setCsrfToken(undefined); setError('Your session has expired. Sign in again.'); };
    window.addEventListener('session-expired', expire);
    const stopListening = listenForSignOut(expire);
    return () => { window.removeEventListener('session-expired', expire); stopListening(); };
  }, []);

  async function signIn(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError('');
    try {
      const token = await refreshCsrf();
      const response = await fetch('/api/v1/auth/login', {
        method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded', [token.headerName]: token.token },
        body: new URLSearchParams({ username: login, password }),
      });
      setPassword('');
      if (!response.ok) { setError('Unable to sign in. Check your login and password, or wait a minute before retrying.'); return; }
      await loadSession();
    } catch { setError('Cannot reach the server. Please retry.'); }
    finally { setPassword(''); setBusy(false); }
  }
  async function signOut() {
    setBusy(true); setError('');
    try { await api('/api/v1/auth/logout', { method: 'POST' }); sessionVersion.current++; announceSignOut(); setIdentity(null); setPassword(''); await refreshCsrf(); }
    catch { setError('Sign out failed. Please retry.'); }
    finally { setBusy(false); }
  }
  if (loading) return <main className="auth-panel" aria-busy="true">Checking your session…</main>;
  return <>
    {error && <div className="auth-panel" role="alert">{error}<button type="button" disabled={busy} onClick={() => void loadSession()}>Retry session</button></div>}
    {identity ? <>
      <header className="auth-panel"><span>Signed in as {identity.login}</span><button disabled={busy} onClick={() => void signOut()}>Sign out</button></header>
      <AccountProfile />
      {identity.authorities.includes('ROLE_HR') ? <App /> : identity.authorities.includes('ROLE_ADMIN') ? <AccountProvisioning /> : <main className="auth-panel"><h1>Access denied</h1><p>Your account does not have access to the employee directory. Scoped employee access is not yet available.</p></main>}
    </> : <main className="auth-panel"><h1>Sign in</h1><p>Use your provisioned account. Public registration is not available.</p>
      <form onSubmit={event => void signIn(event)}>
        <label>Login<input autoComplete="username" required maxLength={64} value={login} onChange={event => setLogin(event.target.value)} /></label>
        <label>Password<input type="password" autoComplete="current-password" required value={password} onChange={event => setPassword(event.target.value)} /></label>
        <button disabled={busy} type="submit">{busy ? 'Signing in…' : 'Sign in'}</button>
      </form></main>}
  </>;
}

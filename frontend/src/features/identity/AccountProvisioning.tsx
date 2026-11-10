import { useRef, useState, type FormEvent } from 'react';
import { api } from '../../lib/api';

type Role = 'ADMIN' | 'HR' | 'MANAGER' | 'EMPLOYEE';
type CreatedAccount = { id: number; login: string; role: Role };

export default function AccountProvisioning() {
  const submitting = useRef(false);
  const [login, setLogin] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<Role>('EMPLOYEE');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [created, setCreated] = useState<CreatedAccount | null>(null);
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (submitting.current) return;
    submitting.current = true; setBusy(true); setError(''); setCreated(null);
    try {
      const result = await api<CreatedAccount>('/api/v1/admin/accounts', {
        method: 'POST', body: JSON.stringify({ login, password, role }),
      });
      setCreated(result); setLogin(''); setRole('EMPLOYEE');
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'Unable to create account. Please retry.');
    } finally { setPassword(''); submitting.current = false; setBusy(false); }
  }
  return <main className="auth-panel">
    <h1>Create account</h1>
    <p>Provision an account only for an authorized user. ADMIN manages accounts; HR manages employee records. Manager and employee data access remains disabled until ownership is configured.</p>
    <p>This screen creates accounts only. Password changes, deactivation and recovery are not yet available. No invitation or password is sent.</p>
    {error && <p role="alert">{error}</p>}
    {created && <p role="status">Created {created.login} with role {created.role}.</p>}
    <form aria-label="Create account" onSubmit={event => void submit(event)}>
      <fieldset disabled={busy} className="account-fields">
        <label>Account login<input autoComplete="off" required maxLength={64} pattern="[A-Za-z0-9][A-Za-z0-9._\-]{2,63}" value={login} onChange={event => setLogin(event.target.value)} /></label>
        <p>Use 3–64 letters, numbers, dots, underscores or hyphens, starting with a letter or number.</p>
        <label>Initial password<input type="password" autoComplete="new-password" required value={password} onChange={event => setPassword(event.target.value)} aria-describedby="password-policy" /></label>
        <p id="password-policy">Use at least 15 characters and no more than 72 UTF-8 bytes. Passwords clear after every submission.</p>
        <label>Account role<select value={role} onChange={event => setRole(event.target.value as Role)}>
          <option value="EMPLOYEE">Employee</option><option value="MANAGER">Manager</option><option value="HR">HR</option><option value="ADMIN">Admin</option>
        </select></label>
        <button type="submit">{busy ? 'Creating account…' : 'Create account'}</button>
      </fieldset>
    </form>
  </main>;
}

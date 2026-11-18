import {useState,type FormEvent} from 'react';
import {api,setCsrfToken} from '../../lib/api';
export default function PasswordReset() {
  const [token,setToken]=useState('');const [password,setPassword]=useState('');
  const [busy,setBusy]=useState(false);const [message,setMessage]=useState('');
  async function submit(event:FormEvent) {
    event.preventDefault();setBusy(true);setMessage('');
    try {
      setCsrfToken(await api<{headerName:string;token:string}>('/api/v1/auth/csrf'));
      await api('/api/v1/auth/reset',{method:'POST',body:JSON.stringify({token,newPassword:password})});
      setMessage('Password reset. Sign in with your new password.');
    } catch {setMessage('Unable to reset password. Ask an administrator for a fresh reset token.');}
    finally {setToken('');setPassword('');setBusy(false);}
  }
  return <section className="auth-panel" aria-label="Password recovery"><h2>Reset password</h2>
    <p>Ask an administrator to verify your identity and issue a reset token. No email is sent by this application.</p>
    <form onSubmit={event=>void submit(event)}>
      <label>Reset token<input type="password" autoComplete="off" required maxLength={64} value={token} onChange={e=>setToken(e.target.value)}/></label>
      <label>Replacement password<input type="password" autoComplete="new-password" required value={password} onChange={e=>setPassword(e.target.value)}/></label>
      <button disabled={busy}>Reset password</button>
    </form>{message&&<p role="status">{message}</p>}
  </section>;
}

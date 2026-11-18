import {useState,type FormEvent} from 'react';
import {api} from '../../lib/api';
import {announceSignOut} from './sessionEvents';
export default function AccountSecurity() {
  const [current,setCurrent]=useState('');const [next,setNext]=useState('');
  const [busy,setBusy]=useState(false);const [message,setMessage]=useState('');
  async function submit(event:FormEvent) {
    event.preventDefault();setBusy(true);setMessage('');
    try {
      await api('/api/v1/auth/password',{method:'POST',body:JSON.stringify({currentPassword:current,newPassword:next})});
      announceSignOut();window.dispatchEvent(new Event('session-expired'));
    } catch {setMessage('Password change failed. Check your current password and try again.');}
    finally {setCurrent('');setNext('');setBusy(false);}
  }
  return <section className="auth-panel" aria-label="Account security"><h2>Change password</h2>
    <p>Use at least 15 characters and at most 72 UTF-8 bytes. Changing your password signs out all sessions.</p>
    <form onSubmit={event=>void submit(event)}>
      <label>Current password<input type="password" autoComplete="current-password" required value={current} onChange={e=>setCurrent(e.target.value)}/></label>
      <label>New password<input type="password" autoComplete="new-password" required value={next} onChange={e=>setNext(e.target.value)}/></label>
      <button disabled={busy}>Change password</button>
    </form>{message&&<p role="alert">{message}</p>}
  </section>;
}

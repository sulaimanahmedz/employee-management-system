import {useState} from 'react';
import {api} from '../../lib/api';
import SecurityEventReview from './SecurityEventReview';
type Access={id:number;login:string;role:string;enabled:boolean};
type Scope={employeeId:number|null;teamEmployeeIds:number[]};
export default function AccountManagement() {
  const [id,setId]=useState('');const [account,setAccount]=useState<Access|null>(null);
  const [role,setRole]=useState('EMPLOYEE');const [enabled,setEnabled]=useState(true);
  const [employee,setEmployee]=useState('');const [team,setTeam]=useState('');
  const [ticket,setTicket]=useState('');const [message,setMessage]=useState('');const [busy,setBusy]=useState(false);
  async function run(action:()=>Promise<void>) {
    setBusy(true);setMessage('');setTicket('');
    try {await action();} catch {setMessage('Account operation failed. Check the account, permissions and submitted values.');}
    finally {setBusy(false);}
  }
  async function load() {
    setAccount(null);
    const value=await api<Access>(`/api/v1/admin/accounts/${id}`);
    const scope=await api<Scope>(`/api/v1/admin/accounts/${id}/scope`);
    setAccount(value);setRole(value.role);setEnabled(value.enabled);
    setEmployee(scope.employeeId===null?'':String(scope.employeeId));setTeam(scope.teamEmployeeIds.join(','));
  }
  return <section className="auth-panel" aria-label="Manage account"><h2>Manage existing account</h2>
    <label>Account to manage<input type="number" min="1" value={id} disabled={busy} onChange={e=>{setId(e.target.value);setAccount(null);setTicket('');}}/></label>
    <button disabled={busy||!id} onClick={()=>void run(load)}>Load account</button>
    {account&&<><p>Managing {account.login}</p>
      <label>Assigned role<select disabled={busy} value={role} onChange={e=>setRole(e.target.value)}>{['ADMIN','HR','MANAGER','EMPLOYEE'].map(r=><option key={r}>{r}</option>)}</select></label>
      <label><input type="checkbox" disabled={busy} checked={enabled} onChange={e=>setEnabled(e.target.checked)}/>Account active</label>
      <button disabled={busy} onClick={()=>void run(async()=>{await api(`/api/v1/admin/accounts/${account.id}`,{method:'PUT',body:JSON.stringify({role,enabled})});setMessage('Account access saved. Existing sessions are revoked.');})}>Save account access</button>
      <label>Linked employee ID<input type="number" min="1" disabled={busy} value={employee} onChange={e=>setEmployee(e.target.value)}/></label>
      <label>Team employee IDs<input disabled={busy} value={team} onChange={e=>setTeam(e.target.value)} placeholder="Comma-separated IDs; managers only"/></label>
      <button disabled={busy} onClick={()=>void run(async()=>{await api(`/api/v1/admin/accounts/${account.id}/scope`,{method:'PUT',body:JSON.stringify({employeeId:employee?Number(employee):null,teamEmployeeIds:team.split(',').filter(Boolean).map(Number)})});setMessage('Employee scope saved. Existing sessions are revoked.');})}>Save employee scope</button>
      <button disabled={busy} onClick={()=>void run(async()=>{const result=await api<{token:string;expiresAt:string}>(`/api/v1/admin/accounts/${account.id}/reset`,{method:'POST'});setTicket(result.token);setMessage(`Single-use reset token expires at ${result.expiresAt}. Verify the recipient before manual handoff.`);})}>Issue reset token</button>
    </>}
    {ticket&&<div><label>Issued reset token<input readOnly value={ticket}/></label><button onClick={()=>setTicket('')}>Dismiss reset token</button></div>}
    <SecurityEventReview/>
    {message&&<p role="status">{message}</p>}
  </section>;
}

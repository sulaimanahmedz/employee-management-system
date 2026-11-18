import {useState} from 'react';
import {api} from '../../lib/api';
type Employee={id:number;firstName:string;lastName:string;email:string;department:string;jobTitle:string;status:string};
export default function ScopedEmployees() {
  const [ids,setIds]=useState<number[]>([]);const [record,setRecord]=useState<Employee|null>(null);
  const [busy,setBusy]=useState(false);const [error,setError]=useState('');
  async function run(action:()=>Promise<void>) {
    setBusy(true);setError('');setRecord(null);
    try {await action();} catch {setError('Unable to load assigned employee data. Please retry.');}
    finally {setBusy(false);}
  }
  return <main className="auth-panel"><h1>{ids.length?'My employee access':'Access denied'}</h1>
    <p>Only explicitly assigned employee profiles can be viewed. All employee changes and related documents remain restricted to HR.</p>
    <button disabled={busy} onClick={()=>void run(async()=>{setIds([]);setIds(await api<number[]>('/api/v1/self/employees'));})}>View assigned employees</button>
    {ids.map(id=><button disabled={busy} key={id} onClick={()=>void run(async()=>setRecord(await api<Employee>(`/api/v1/self/employees/${id}`)))}>View employee {id}</button>)}
    {record&&<section aria-label="Assigned employee"><h2>{record.firstName} {record.lastName}</h2><p>{record.email}</p><p>{record.department} — {record.jobTitle} — {record.status}</p></section>}
    {error&&<p role="alert">{error}</p>}
  </main>;
}

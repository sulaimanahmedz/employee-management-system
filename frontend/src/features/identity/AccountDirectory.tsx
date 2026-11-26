import { useEffect, useState } from 'react';
import { api } from '../../lib/api';

type AccountPage = { items: { id: number; login: string; role: string; enabled: boolean }[]; page: number; totalPages: number; totalElements: number };

export default function AccountDirectory() {
  const [enabled, setEnabled] = useState('');
  const [role, setRole] = useState('');
  const [request, setRequest] = useState<{ page: number } | null>(null);
  const [data, setData] = useState<AccountPage | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  useEffect(() => {
    if (!request) return;
    let current = true;
    const controller = new AbortController();
    setLoading(true); setError(''); setData(null);
    void api<AccountPage>(`/api/v1/admin/accounts?page=${request.page}${role ? `&role=${role}` : ''}${enabled ? `&enabled=${enabled}` : ''}`, { signal: controller.signal })
      .then(result => { if (current) setData(result); })
      .catch(() => { if (current) setError('Unable to load accounts. Please retry.'); })
      .finally(() => { if (current) setLoading(false); });
    return () => { current = false; controller.abort(); };
  }, [request, role, enabled]);
  return <section aria-label="Account directory">
    <h2>Account directory</h2>
    <p>Review account logins and assigned roles. Status reflects the last refresh, not an active session. Passwords are never displayed.</p>
    <label>Filter accounts by role
      <select value={role} disabled={loading} onChange={event => { setRole(event.target.value); setRequest({ page: 0 }); }}>
        <option value="">All roles</option>
        {['ADMIN', 'HR', 'MANAGER', 'EMPLOYEE'].map(value => <option key={value} value={value}>{value}</option>)}
      </select>
    </label>
    <label>Filter accounts by status
      <select value={enabled} disabled={loading} onChange={event => { setEnabled(event.target.value); setRequest({ page: 0 }); }}>
        <option value="">All statuses</option>
        <option value="true">Active</option>
        <option value="false">Deactivated</option>
      </select>
    </label>
    <button type="button" disabled={loading} onClick={() => setRequest({ page: 0 })}>View accounts</button>
    {loading && <p role="status">Loading accounts…</p>}
    {error && <p role="alert">{error}</p>}
    {data && <>
      <p>{data.totalElements} accounts · Page {data.page + 1} of {Math.max(1, data.totalPages)}</p>
      {data.items.length === 0 ? <p>No accounts on this page. Select View accounts to refresh.</p> : <ul>
        {data.items.map(account => <li key={account.id}><strong>{account.login}</strong> — {account.role} (ID {account.id}) — {account.enabled ? 'Active' : 'Deactivated'}</li>)}
      </ul>}
      <nav aria-label="Account pages">
        <button type="button" disabled={data.page === 0} onClick={() => setRequest({ page: data.page - 1 })}>Previous accounts</button>
        <button type="button" disabled={data.page + 1 >= data.totalPages} onClick={() => setRequest({ page: data.page + 1 })}>Next accounts</button>
      </nav>
    </>}
  </section>;
}

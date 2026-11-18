import { useEffect, useState } from 'react';
import { api } from '../../lib/api';

type AccountPage = { items: { id: number; login: string; role: string }[]; page: number; totalPages: number; totalElements: number };

export default function AccountDirectory() {
  const [request, setRequest] = useState<{ page: number } | null>(null);
  const [data, setData] = useState<AccountPage | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  useEffect(() => {
    if (!request) return;
    let current = true;
    const controller = new AbortController();
    setLoading(true); setError(''); setData(null);
    void api<AccountPage>(`/api/v1/admin/accounts?page=${request.page}`, { signal: controller.signal })
      .then(result => { if (current) setData(result); })
      .catch(() => { if (current) setError('Unable to load accounts. Please retry.'); })
      .finally(() => { if (current) setLoading(false); });
    return () => { current = false; controller.abort(); };
  }, [request]);
  return <section aria-label="Account directory">
    <h2>Account directory</h2>
    <p>Review account logins and assigned roles. Passwords are never displayed.</p>
    <button type="button" disabled={loading} onClick={() => setRequest({ page: 0 })}>View accounts</button>
    {loading && <p role="status">Loading accounts…</p>}
    {error && <p role="alert">{error}</p>}
    {data && <>
      <p>{data.totalElements} accounts · Page {data.page + 1} of {Math.max(1, data.totalPages)}</p>
      {data.items.length === 0 ? <p>No accounts on this page. Select View accounts to refresh.</p> : <ul>
        {data.items.map(account => <li key={account.id}><strong>{account.login}</strong> — {account.role} (ID {account.id})</li>)}
      </ul>}
      <nav aria-label="Account pages">
        <button type="button" disabled={data.page === 0} onClick={() => setRequest({ page: data.page - 1 })}>Previous accounts</button>
        <button type="button" disabled={data.page + 1 >= data.totalPages} onClick={() => setRequest({ page: data.page + 1 })}>Next accounts</button>
      </nav>
    </>}
  </section>;
}

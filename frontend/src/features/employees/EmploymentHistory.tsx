import { useEffect, useState } from 'react';
import { getEmploymentHistory } from './api';
import type { EmploymentHistoryPage } from './types';

const kinds = { CREATED: 'Profile created', BASELINE: 'Existing profile baseline', CHANGED: 'Employment details changed' };
const statuses = { ACTIVE: 'Active', ON_LEAVE: 'On leave', INACTIVE: 'Inactive' };
export default function EmploymentHistory({ employeeId }: { employeeId: number }) {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<EmploymentHistoryPage | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true); setError(''); setData(null);
    getEmploymentHistory(employeeId, page, controller.signal).then(result => {
      if (!controller.signal.aborted) setData(result);
    }).catch((cause: unknown) => {
      if (!controller.signal.aborted) setError(cause instanceof Error ? cause.message : 'Unable to load history.');
    }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [employeeId, page, retry]);
  return <section className="panel" aria-labelledby="history-heading">
    <h2 id="history-heading">Employment history</h2>
    <p className="muted">Recorded profile changes, newest first. Times show when changes were saved, not employment effective dates. Earlier history is not reconstructed.</p>
    {loading && <p role="status">Loading employment history…</p>}
    {error && <p className="error" role="alert">{error} <button type="button" onClick={() => setRetry(value => value + 1)}>Retry history</button></p>}
    {data && <>
      {data.totalElements === 0 ? <p>No history recorded yet. A baseline will be recorded on the next profile save.</p> :
        <ol>{data.items.map(entry => <li key={entry.id}>
          <strong>{kinds[entry.kind]}</strong>{' — '}
          <time dateTime={entry.recordedAt}>{new Date(entry.recordedAt).toLocaleString()}</time>
          <p>{entry.department} · {entry.jobTitle} · {statuses[entry.status]}</p>
        </li>)}</ol>}
      {data.totalPages > 1 && <nav aria-label="Employment history pages">
        <button type="button" disabled={page === 0} onClick={() => setPage(value => value - 1)}>Previous history</button>
        <span> Page {page + 1} of {data.totalPages} </span>
        <button type="button" disabled={page + 1 >= data.totalPages} onClick={() => setPage(value => value + 1)}>Next history</button>
      </nav>}
    </>}
  </section>;
}

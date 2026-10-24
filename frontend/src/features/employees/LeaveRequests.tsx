import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { api } from '../../lib/api';

type LeaveType = 'ANNUAL' | 'PERSONAL' | 'UNPAID';
interface LeaveRequest { id: number; type: LeaveType; startOn: string; endOn: string; status: 'PENDING' | 'CANCELLED'; calendarDays: number }
interface LeavePage { items: LeaveRequest[]; page: number; totalPages: number; totalElements: number }
const emptyFilter = { status: '', from: '', to: '' };
const labels: Record<LeaveType, string> = { ANNUAL: 'Annual', PERSONAL: 'Personal', UNPAID: 'Unpaid' };

export default function LeaveRequests({ employeeId }: { employeeId: number }) {
  const [data, setData] = useState<LeavePage | null>(null);
  const [filterDraft, setFilterDraft] = useState(emptyFilter);
  const [filters, setFilters] = useState(emptyFilter);
  const [page, setPage] = useState(0);
  const [reload, setReload] = useState(0);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [type, setType] = useState<LeaveType>('ANNUAL');
  const [startOn, setStartOn] = useState('');
  const [endOn, setEndOn] = useState('');
  const pending = useRef(false);
  const path = `/api/v1/employees/${employeeId}/leave-requests`;
  useEffect(() => {
    const controller = new AbortController(); setLoading(true); setError('');
    const params = new URLSearchParams({ page: String(page) });
    Object.entries(filters).forEach(([key, value]) => { if (value) params.set(key, value); });
    api<LeavePage>(`${path}?${params}`, { signal: controller.signal }).then(result => {
      if (controller.signal.aborted) return;
      const last = Math.max(0, result.totalPages - 1);
      if (page > last) { setPage(last); return; }
      setData(result);
    }).catch((cause: unknown) => {
      if (!controller.signal.aborted) setError(cause instanceof Error ? cause.message : 'Unable to load leave requests.');
    }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [path, page, reload, filters]);
  async function mutate(action: () => Promise<unknown>, message: string, created = false) {
    if (pending.current) return;
    pending.current = true; setBusy(true); setError(''); setNotice('');
    try {
      await action(); setNotice(message);
      if (created) { setStartOn(''); setEndOn(''); setPage(0); }
      setReload(value => value + 1);
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Unable to save leave request.'); }
    finally { pending.current = false; setBusy(false); }
  }
  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (filterDraft.from && filterDraft.to && filterDraft.to < filterDraft.from) {
      setError('Filter end date must be on or after filter start date.'); return;
    }
    setFilters({ ...filterDraft }); setPage(0); setNotice('');
  }
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!startOn || !endOn) return;
    if (endOn < startOn) { setError('End date must be on or after start date.'); return; }
    void mutate(() => api<LeaveRequest>(path, { method: 'POST', body: JSON.stringify({ type, startOn, endOn }) }), 'Leave request recorded as pending.', true);
  }
  return <section className="panel" aria-labelledby="leave-heading">
    <h2 id="leave-heading">Leave requests</h2>
    <p className="muted">Record full-day requests. Dates include weekends and holidays; approval and leave balances are not yet available. Requests do not change employee status.</p>
    {error && <p role="alert" className="error">{error} <button type="button" disabled={busy || loading} onClick={() => setReload(value => value + 1)}>Refresh leave requests</button></p>}
    {notice && <p role="status">{notice}</p>}
    <form onSubmit={applyFilters} aria-label="Filter leave requests">
      <fieldset disabled={busy} className="profile-fieldset"><legend>Filter leave requests</legend>
        <div className="form-grid">
          <label>Request status<select value={filterDraft.status} onChange={event => setFilterDraft({ ...filterDraft, status: event.target.value })}>
            <option value="">All statuses</option><option value="PENDING">Pending</option><option value="CANCELLED">Cancelled</option>
          </select></label>
          <label>Period from<input type="date" value={filterDraft.from} onChange={event => setFilterDraft({ ...filterDraft, from: event.target.value })} /></label>
          <label>Period to<input type="date" min={filterDraft.from || undefined} value={filterDraft.to} onChange={event => setFilterDraft({ ...filterDraft, to: event.target.value })} /></label>
        </div>
        <p className="muted">Matches requests overlapping any day in this period. Either date can be left empty.</p>
        <button type="submit">Apply leave filters</button>{' '}
        <button type="button" onClick={() => { setFilterDraft(emptyFilter); setFilters(emptyFilter); setPage(0); setNotice(''); }}>Clear leave filters</button>
      </fieldset>
    </form>
    {(filters.status || filters.from || filters.to) && <p role="status">Showing {filters.status ? filters.status.toLowerCase() : 'all'} requests overlapping {filters.from || 'any start date'} through {filters.to || 'any end date'}. New requests may be outside these filters.</p>}
    {loading ? <p role="status">Loading leave requests…</p> : data && <>
      <p>{data.totalElements} matching leave requests</p>
      {data.items.length === 0 ? <p>No leave requests match the applied filters.</p> : <ul>{data.items.map(request => <li key={request.id}>
        <strong>{labels[request.type]} leave</strong> — <time dateTime={request.startOn}>{request.startOn}</time> to <time dateTime={request.endOn}>{request.endOn}</time>
        {' '}({request.calendarDays} calendar {request.calendarDays === 1 ? 'day' : 'days'}) — {request.status === 'PENDING' ? 'Pending' : 'Cancelled'}{' '}
        {request.status === 'PENDING' && <button type="button" disabled={busy} aria-label={`Cancel leave request starting ${request.startOn}`} onClick={() => {
          if (window.confirm(`Cancel leave from ${request.startOn} to ${request.endOn}?`))
            void mutate(() => api<LeaveRequest>(`${path}/${request.id}/cancellation`, { method: 'PUT' }), 'Leave request cancelled.');
        }}>Cancel request</button>}
      </li>)}</ul>}
      {data.totalPages > 1 && <nav aria-label="Leave request pages">
        <button type="button" disabled={busy || page === 0} onClick={() => setPage(value => value - 1)}>Previous requests</button>
        <span> Page {page + 1} of {data.totalPages} </span>
        <button type="button" disabled={busy || page + 1 >= data.totalPages} onClick={() => setPage(value => value + 1)}>Next requests</button>
      </nav>}
    </>}
    <form onSubmit={submit} aria-label="Record leave request">
      <fieldset disabled={busy || loading} className="profile-fieldset"><legend>Record leave request</legend>
        <div className="form-grid">
          <label>Leave type<select value={type} onChange={event => setType(event.target.value as LeaveType)}>
            {Object.entries(labels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select></label>
          <label>Start date<input type="date" required value={startOn} onChange={event => setStartOn(event.target.value)} /></label>
          <label>End date<input type="date" required min={startOn || undefined} value={endOn} onChange={event => setEndOn(event.target.value)} /></label>
        </div>
        <button type="submit" disabled={!startOn || !endOn || endOn < startOn}>{busy ? 'Saving…' : 'Record request'}</button>
      </fieldset>
    </form>
  </section>;
}

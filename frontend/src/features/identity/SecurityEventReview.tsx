import { useEffect, useState } from 'react';
import { api } from '../../lib/api';

type SecurityEvent = { id: number; occurredAt: string; action: string; actorId: number | null; subjectId: number | null };

export default function SecurityEventReview() {
  const [request, setRequest] = useState<{ page: number } | null>(null);
  const [events, setEvents] = useState<SecurityEvent[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  useEffect(() => {
    if (!request) return;
    let current = true;
    const controller = new AbortController();
    setLoading(true); setEvents(null); setError('');
    void api<SecurityEvent[]>(`/api/v1/admin/audit?page=${request.page}`, { signal: controller.signal })
      .then(value => { if (current) setEvents(value); })
      .catch(() => { if (current) setError('Unable to load security events. Please retry.'); })
      .finally(() => { if (current) setLoading(false); });
    return () => { current = false; controller.abort(); };
  }, [request]);
  return <section aria-label="Security event review">
    <h2>Security events</h2>
    <p>Newest first, up to 20 events per page. New activity can shift pages; refresh to see the latest events.</p>
    <button disabled={loading} onClick={() => setRequest({ page: 0 })}>View recent security events</button>
    {loading && <p role="status">Loading security events…</p>}
    {error && <><p role="alert">{error}</p><button onClick={() => setRequest({ page: request!.page })}>Retry security events</button></>}
    {events && <>
      <p role="status">Security events · Page {request!.page + 1}</p>
      {events.length === 0 ? <p>No security events on this page.</p> : <ul>{events.map(event =>
        <li key={event.id}>Event {event.id}: {event.occurredAt} — {event.action} — actor {event.actorId ?? 'unattributed'} — subject {event.subjectId ?? 'unattributed'}</li>)}</ul>}
      <nav aria-label="Security event pages" className="security-event-pages">
        <button disabled={request!.page === 0} onClick={() => setRequest({ page: request!.page - 1 })}>Newer security events</button>
        <button disabled={events.length < 20} onClick={() => setRequest({ page: request!.page + 1 })}>Older security events</button>
      </nav>
    </>}
  </section>;
}

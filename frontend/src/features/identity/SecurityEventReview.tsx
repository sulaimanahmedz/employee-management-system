import { useEffect, useState } from 'react';
import { api } from '../../lib/api';

type SecurityEvent = { id: number; occurredAt: string; action: string; actorId: number | null; subjectId: number | null };

export default function SecurityEventReview() {
  const [actor, setActor] = useState('');
  const [actorId, setActorId] = useState('');
  const [subject, setSubject] = useState('');
  const [subjectId, setSubjectId] = useState('');
  const [action, setAction] = useState('');
  const [request, setRequest] = useState<{ page: number } | null>(null);
  const [events, setEvents] = useState<SecurityEvent[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  useEffect(() => {
    if (!request) return;
    let current = true;
    const controller = new AbortController();
    setLoading(true); setEvents(null); setError('');
    void api<SecurityEvent[]>(`/api/v1/admin/audit?page=${request.page}${action ? `&action=${action}` : ''}${subjectId ? `&subjectId=${subjectId}` : ''}${actorId ? `&actorId=${actorId}` : ''}`, { signal: controller.signal })
      .then(value => { if (current) setEvents(value); })
      .catch(() => { if (current) setError('Unable to load security events. Please retry.'); })
      .finally(() => { if (current) setLoading(false); });
    return () => { current = false; controller.abort(); };
  }, [request, action, subjectId, actorId]);
  return <section aria-label="Security event review">
    <h2>Security events</h2>
    <p>Newest first, up to 20 events per page. New activity can shift pages; refresh to see the latest events.</p>
    <label>Security event action<select disabled={loading} value={action} onChange={event => { setAction(event.target.value); setRequest({ page: 0 }); }}>
      <option value="">All actions</option>
      {['PASSWORD_CHANGED','ACCOUNT_ACCESS_CHANGED','RESET_ISSUED','PASSWORD_RESET','LINK_CHANGED','TEAM_CHANGED','CREDENTIALS_ACCEPTED','CREDENTIALS_REJECTED','VERIFICATION_UNAVAILABLE','SESSION_ESTABLISHED','LOGOUT_PROCESSED','AUTHENTICATION_REQUIRED','ACCESS_DENIED'].map(value => <option key={value} value={value}>{value.replaceAll('_',' ')}</option>)}
    </select></label>
    <form onSubmit={event => { event.preventDefault(); setSubjectId(subject); setRequest({ page: 0 }); }}>
      <label>Subject account ID<input inputMode="numeric" pattern="[1-9][0-9]*" maxLength={19} value={subject} disabled={loading} onChange={event => setSubject(event.target.value)} /></label>
      <button disabled={loading}>Apply subject filter</button>
      <button type="button" disabled={loading} onClick={() => { setSubject(''); setSubjectId(''); setRequest({ page: 0 }); }}>Clear subject filter</button>
    </form>
    <p>Subject filter: {subjectId || 'All subjects'}. This identifies the affected account, not the actor.</p>
    <form onSubmit={event => { event.preventDefault(); setActorId(actor); setRequest({ page: 0 }); }}>
      <label>Actor account ID<input inputMode="numeric" pattern="[1-9][0-9]*" maxLength={19} value={actor} disabled={loading} onChange={event => setActor(event.target.value)} /></label>
      <button disabled={loading}>Apply actor filter</button>
      <button type="button" disabled={loading} onClick={() => { setActor(''); setActorId(''); setRequest({ page: 0 }); }}>Clear actor filter</button>
    </form>
    <p>Actor filter: {actorId || 'All actors'}. This identifies who performed the recorded action.</p>
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

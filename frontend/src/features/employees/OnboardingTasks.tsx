import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { api } from '../../lib/api';
interface Task { id: number; title: string; owner: string; dueOn: string | null; completed: boolean }
interface TaskPage { items: Task[]; page: number; totalPages: number; totalElements: number; completedTasks: number }

export default function OnboardingTasks({ employeeId }: { employeeId: number }) {
  const [data, setData] = useState<TaskPage | null>(null);
  const [page, setPage] = useState(0);
  const [reload, setReload] = useState(0);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [title, setTitle] = useState('');
  const [owner, setOwner] = useState('');
  const [dueOn, setDueOn] = useState('');
  const pending = useRef(false);
  const path = `/api/v1/employees/${employeeId}/onboarding-tasks`;
  useEffect(() => {
    const controller = new AbortController(); setLoading(true); setError('');
    api<TaskPage>(`${path}?page=${page}`, { signal: controller.signal }).then(result => {
      if (controller.signal.aborted) return;
      const last = Math.max(0, result.totalPages - 1);
      if (page > last) { setPage(last); return; }
      setData(result);
    }).catch((cause: unknown) => {
      if (!controller.signal.aborted) setError(cause instanceof Error ? cause.message : 'Unable to load checklist.');
    }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [path, page, reload]);
  async function mutate(action: () => Promise<unknown>, message: string, added = false) {
    if (pending.current) return;
    pending.current = true; setBusy(true); setError(''); setNotice('');
    try {
      await action(); setNotice(message);
      if (added) { setTitle(''); setOwner(''); setDueOn(''); setPage(0); }
      setReload(value => value + 1);
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Unable to save task.'); }
    finally { pending.current = false; setBusy(false); }
  }
  function add(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!title.trim() || !owner.trim()) return;
    void mutate(() => api<Task>(path, { method: 'POST', body: JSON.stringify({ title: title.trim(), owner: owner.trim(), dueOn: dueOn || null }) }), 'Task added.', true);
  }
  return <section className="panel" aria-labelledby="onboarding-heading">
    <h2 id="onboarding-heading">Onboarding checklist</h2>
    <p className="muted">Owner labels identify who should handle a task; they do not grant access or send notifications.</p>
    {error && <p role="alert" className="error">{error} <button type="button" disabled={busy || loading} onClick={() => setReload(value => value + 1)}>Refresh checklist</button></p>}
    {notice && <p role="status">{notice}</p>}
    {loading ? <p role="status">Loading checklist…</p> : data && <>
      <p>{data.completedTasks} of {data.totalElements} tasks complete</p>
      {data.totalElements === 0 ? <p>No onboarding tasks yet.</p> : <ul>{data.items.map(task => <li key={task.id}>
        <strong>{task.title}</strong> — {task.owner} — {task.completed ? 'Complete' : 'Pending'}
        {task.dueOn && <> — Due <time dateTime={task.dueOn}>{task.dueOn}</time></>}{' '}
        <button type="button" disabled={busy} aria-label={`${task.completed ? 'Reopen' : 'Complete'} ${task.title}`}
          onClick={() => void mutate(() => api<Task>(`${path}/${task.id}/completion`, { method: 'PUT', body: JSON.stringify({ completed: !task.completed }) }), 'Task updated.')}>{task.completed ? 'Reopen' : 'Complete'}</button>{' '}
        <button type="button" disabled={busy} aria-label={`Remove task ${task.title}`} onClick={() => {
          if (window.confirm(`Remove task "${task.title}"?`)) void mutate(() => api<void>(`${path}/${task.id}`, { method: 'DELETE' }), 'Task removed.');
        }}>Remove</button>
      </li>)}</ul>}
      {data.totalPages > 1 && <nav aria-label="Onboarding checklist pages">
        <button type="button" disabled={busy || page === 0} onClick={() => setPage(value => value - 1)}>Previous tasks</button>
        <span> Page {page + 1} of {data.totalPages} </span>
        <button type="button" disabled={busy || page + 1 >= data.totalPages} onClick={() => setPage(value => value + 1)}>Next tasks</button>
      </nav>}
    </>}
    <form onSubmit={add} aria-label="Add onboarding task">
      <fieldset disabled={busy || loading} className="profile-fieldset"><legend>Add task</legend>
        <div className="form-grid">
          <label>Task title<input required maxLength={150} value={title} onChange={event => setTitle(event.target.value)} /></label>
          <label>Owner label<input required maxLength={100} value={owner} onChange={event => setOwner(event.target.value)} /></label>
          <label>Due date (optional)<input type="date" value={dueOn} onChange={event => setDueOn(event.target.value)} /></label>
        </div>
        <button type="submit" disabled={!title.trim() || !owner.trim()}>{busy ? 'Saving…' : 'Add task'}</button>
      </fieldset>
    </form>
  </section>;
}

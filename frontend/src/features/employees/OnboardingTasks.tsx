import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { api } from '../../lib/api';
interface Task { id: number; title: string; owner: string; dueOn: string | null; completed: boolean }
interface TaskPage { items: Task[]; page: number; totalPages: number; totalElements: number; completedTasks: number; totalTasks: number }

export default function OnboardingTasks({ employeeId }: { employeeId: number }) {
  const [data, setData] = useState<TaskPage | null>(null);
  const [filter, setFilter] = useState('');
  const [page, setPage] = useState(0);
  const [reload, setReload] = useState(0);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [editing, setEditing] = useState<Task | null>(null);
  const [title, setTitle] = useState('');
  const [owner, setOwner] = useState('');
  const [dueOn, setDueOn] = useState('');
  const pending = useRef(false);
  const path = `/api/v1/employees/${employeeId}/onboarding-tasks`;
  useEffect(() => {
    const controller = new AbortController(); setLoading(true); setError('');
    api<TaskPage>(`${path}?page=${page}${filter ? `&completed=${filter}` : ''}`, { signal: controller.signal }).then(result => {
      if (controller.signal.aborted) return;
      const last = Math.max(0, result.totalPages - 1);
      if (page > last) { setPage(last); return; }
      setData(result);
    }).catch((cause: unknown) => {
      if (!controller.signal.aborted) setError(cause instanceof Error ? cause.message : 'Unable to load checklist.');
    }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [path, page, reload, filter]);
  async function mutate(action: () => Promise<unknown>, message: string, added = false) {
    if (pending.current) return;
    pending.current = true; setBusy(true); setError(''); setNotice('');
    try {
      await action(); setNotice(message);
      if (added) { setEditing(null); setTitle(''); setOwner(''); setDueOn(''); setPage(0); }
      setReload(value => value + 1);
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Unable to save task.'); }
    finally { pending.current = false; setBusy(false); }
  }
  function add(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!title.trim() || !owner.trim()) return;
    void mutate(() => api<Task>(editing ? `${path}/${editing.id}` : path, { method: editing ? 'PUT' : 'POST', body: JSON.stringify({ title: title.trim(), owner: owner.trim(), dueOn: dueOn || null }) }), editing ? 'Task details updated.' : 'Task added.', true);
  }
  function beginEdit(task: Task) {
    if ((title || owner || dueOn) && !window.confirm('Discard the current task draft?')) return;
    setEditing(task); setTitle(task.title); setOwner(task.owner); setDueOn(task.dueOn ?? '');
    setError(''); setNotice('');
  }
  function cancelEdit() {
    const changed = editing && (title !== editing.title || owner !== editing.owner || dueOn !== (editing.dueOn ?? ''));
    if (changed && !window.confirm('Discard your unsaved task changes?')) return;
    setEditing(null); setTitle(''); setOwner(''); setDueOn(''); setError('');
  }
  return <section className="panel" aria-labelledby="onboarding-heading">
    <h2 id="onboarding-heading">Onboarding checklist</h2>
    <p className="muted">Owner labels identify who should handle a task; they do not grant access or send notifications.</p>
    {error && <p role="alert" className="error">{error} <button type="button" disabled={busy || loading} onClick={() => setReload(value => value + 1)}>Refresh checklist</button></p>}
    {notice && <p role="status">{notice}</p>}
    <label>Show onboarding tasks
      <select value={filter} disabled={busy || editing !== null} onChange={event => { setFilter(event.target.value); setPage(0); }}>
        <option value="">All tasks</option><option value="false">Pending tasks</option><option value="true">Completed tasks</option>
      </select>
    </label>
    {loading ? <p role="status">Loading checklist…</p> : data && <>
      <p>{data.completedTasks} of {data.totalTasks} tasks complete · {data.totalElements} matching tasks</p>
      {data.totalElements === 0 ? <p>{filter ? 'No tasks match this filter.' : 'No onboarding tasks yet.'}</p> : <ul>{data.items.map(task => <li key={task.id}>
        <strong>{task.title}</strong> — {task.owner} — {task.completed ? 'Complete' : 'Pending'}
        {task.dueOn && <> — Due <time dateTime={task.dueOn}>{task.dueOn}</time></>}{' '}
        <button type="button" disabled={busy} aria-label={`Edit task ${task.title}`} onClick={() => beginEdit(task)}>Edit</button>{' '}
        <button type="button" disabled={busy || editing !== null} aria-label={`${task.completed ? 'Reopen' : 'Complete'} ${task.title}`}
          onClick={() => void mutate(() => api<Task>(`${path}/${task.id}/completion`, { method: 'PUT', body: JSON.stringify({ completed: !task.completed }) }), 'Task updated.')}>{task.completed ? 'Reopen' : 'Complete'}</button>{' '}
        <button type="button" disabled={busy || editing !== null} aria-label={`Remove task ${task.title}`} onClick={() => {
          if (window.confirm(`Remove task "${task.title}"?`)) void mutate(() => api<void>(`${path}/${task.id}`, { method: 'DELETE' }), 'Task removed.');
        }}>Remove</button>
      </li>)}</ul>}
      {data.totalPages > 1 && <nav aria-label="Onboarding checklist pages">
        <button type="button" disabled={busy || editing !== null || page === 0} onClick={() => setPage(value => value - 1)}>Previous tasks</button>
        <span> Page {page + 1} of {data.totalPages} </span>
        <button type="button" disabled={busy || editing !== null || page + 1 >= data.totalPages} onClick={() => setPage(value => value + 1)}>Next tasks</button>
      </nav>}
    </>}
    <form onSubmit={add} aria-label={editing ? 'Edit onboarding task' : 'Add onboarding task'}>
      <fieldset disabled={busy || loading} className="profile-fieldset"><legend>{editing ? 'Edit task details' : 'Add task'}</legend>
        <div className="form-grid">
          <label>Task title<input required maxLength={150} value={title} onChange={event => setTitle(event.target.value)} /></label>
          <label>Owner label<input required maxLength={100} value={owner} onChange={event => setOwner(event.target.value)} /></label>
          <label>Due date (optional)<input type="date" value={dueOn} onChange={event => setDueOn(event.target.value)} /></label>
        </div>
        <button type="submit" disabled={!title.trim() || !owner.trim()}>{busy ? 'Saving…' : editing ? 'Save task changes' : 'Add task'}</button>
        {editing && <button type="button" onClick={cancelEdit}>Cancel task edit</button>}
      </fieldset>
    </form>
  </section>;
}

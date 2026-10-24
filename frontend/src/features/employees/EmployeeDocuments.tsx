import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { api } from '../../lib/api';

type Category = 'CONTRACT' | 'CERTIFICATE' | 'OTHER';
interface DocumentMetadata { id: number; name: string; category: Category; expiresOn: string | null }
const categories: Record<Category, string> = { CONTRACT: 'Contract', CERTIFICATE: 'Certificate', OTHER: 'Other' };

export default function EmployeeDocuments({ employeeId }: { employeeId: number }) {
  const [documents, setDocuments] = useState<DocumentMetadata[]>([]);
  const [editing, setEditing] = useState<DocumentMetadata | null>(null);
  const [name, setName] = useState('');
  const [category, setCategory] = useState<Category>('CONTRACT');
  const [expiresOn, setExpiresOn] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [reload, setReload] = useState(0);
  const submitting = useRef(false);
  const path = `/api/v1/employees/${employeeId}/documents`;
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true); setError('');
    api<DocumentMetadata[]>(path, { signal: controller.signal }).then(value => {
      if (!controller.signal.aborted) setDocuments(value);
    }).catch((cause: unknown) => {
      if (!controller.signal.aborted) setError(cause instanceof Error ? cause.message : 'Unable to load documents.');
    }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [path, reload]);

  async function add(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting.current || !name.trim()) return;
    submitting.current = true; setBusy(true); setError(''); setNotice('');
    try {
      const saved = await api<DocumentMetadata>(editing ? `${path}/${editing.id}` : path, { method: editing ? 'PUT' : 'POST',
        body: JSON.stringify({ name: name.trim(), category, expiresOn: expiresOn || null }) });
      setDocuments(current => editing ? current.map(item => item.id === saved.id ? saved : item) : [saved, ...current]);
      setName(''); setExpiresOn(''); setCategory('CONTRACT'); setEditing(null);
      setNotice(editing ? 'Document metadata updated.' : 'Document metadata added.');
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Unable to save document.'); }
    finally { submitting.current = false; setBusy(false); }
  }
  function beginEdit(document: DocumentMetadata) {
    if ((name || expiresOn || category !== 'CONTRACT') && !window.confirm('Discard the current document draft?')) return;
    setEditing(document); setName(document.name); setCategory(document.category); setExpiresOn(document.expiresOn ?? '');
    setError(''); setNotice('');
  }
  function cancelEdit() {
    const changed = editing && (name !== editing.name || category !== editing.category || expiresOn !== (editing.expiresOn ?? ''));
    if (changed && !window.confirm('Discard your unsaved document changes?')) return;
    setEditing(null); setName(''); setCategory('CONTRACT'); setExpiresOn(''); setError('');
  }
  async function remove(document: DocumentMetadata) {
    if (submitting.current || !window.confirm(`Remove metadata for "${document.name}"?`)) return;
    submitting.current = true; setBusy(true); setError(''); setNotice('');
    try {
      await api<void>(`${path}/${document.id}`, { method: 'DELETE' });
      setDocuments(current => current.filter(item => item.id !== document.id));
      setNotice('Document metadata removed.');
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Unable to remove document.'); }
    finally { submitting.current = false; setBusy(false); }
  }
  return <section className="panel" aria-labelledby="documents-heading">
    <h2 id="documents-heading">Document register</h2>
    <p className="muted">Track names, categories and expiry dates only. No files or sensitive document numbers are stored.</p>
    {error && <p role="alert" className="error">{error} <button type="button" disabled={busy || loading}
      onClick={() => setReload(value => value + 1)}>Refresh list</button></p>}
    {notice && <p role="status">{notice}</p>}
    {loading ? <p role="status">Loading document register…</p> : documents.length === 0 ?
      <p>No document metadata registered.</p> : <ul>
        {documents.map(document => <li key={document.id}>
          <strong>{document.name}</strong> — {categories[document.category]} — {document.expiresOn ?
            <>Expires <time dateTime={document.expiresOn}>{document.expiresOn}</time></> : 'No expiry date'}{' '}
          <button type="button" disabled={busy} onClick={() => beginEdit(document)} aria-label={`Edit ${document.name}`}>Edit</button>{' '}
          <button type="button" disabled={busy || editing !== null} onClick={() => remove(document)}
            aria-label={`Remove ${document.name}`}>Remove</button>
        </li>)}
      </ul>}
    <form onSubmit={add} aria-label={editing ? 'Edit document metadata' : 'Add document metadata'}>
      <fieldset disabled={busy || loading} className="profile-fieldset">
        <legend>{editing ? 'Edit document metadata' : 'Add document metadata'}</legend>
        <div className="form-grid">
          <label>Document name<input required maxLength={150} value={name} onChange={event => setName(event.target.value)} /></label>
          <label>Category<select value={category} onChange={event => setCategory(event.target.value as Category)}>
            {Object.entries(categories).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select></label>
          <label>Expiry date (optional)<input type="date" value={expiresOn} onChange={event => setExpiresOn(event.target.value)} /></label>
        </div>
        <button type="submit" disabled={!name.trim()}>{busy ? 'Saving…' : editing ? 'Save metadata changes' : 'Add metadata'}</button>
        {editing && <button type="button" onClick={cancelEdit}>Cancel metadata edit</button>}
      </fieldset>
    </form>
  </section>;
}

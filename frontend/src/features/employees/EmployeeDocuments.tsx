import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { api } from '../../lib/api';

type Category = 'CONTRACT' | 'CERTIFICATE' | 'OTHER';
interface DocumentMetadata { id: number; name: string; category: Category; expiresOn: string | null }
const categories: Record<Category, string> = { CONTRACT: 'Contract', CERTIFICATE: 'Certificate', OTHER: 'Other' };

export default function EmployeeDocuments({ employeeId }: { employeeId: number }) {
  const [documents, setDocuments] = useState<DocumentMetadata[]>([]);
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
      const saved = await api<DocumentMetadata>(path, { method: 'POST',
        body: JSON.stringify({ name: name.trim(), category, expiresOn: expiresOn || null }) });
      setDocuments(current => [saved, ...current]);
      setName(''); setExpiresOn(''); setNotice('Document metadata added.');
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Unable to add document.'); }
    finally { submitting.current = false; setBusy(false); }
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
          <button type="button" disabled={busy} onClick={() => remove(document)}
            aria-label={`Remove ${document.name}`}>Remove</button>
        </li>)}
      </ul>}
    <form onSubmit={add} aria-label="Add document metadata">
      <fieldset disabled={busy || loading} className="profile-fieldset">
        <legend>Add document metadata</legend>
        <div className="form-grid">
          <label>Document name<input required maxLength={150} value={name} onChange={event => setName(event.target.value)} /></label>
          <label>Category<select value={category} onChange={event => setCategory(event.target.value as Category)}>
            {Object.entries(categories).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select></label>
          <label>Expiry date (optional)<input type="date" value={expiresOn} onChange={event => setExpiresOn(event.target.value)} /></label>
        </div>
        <button type="submit" disabled={!name.trim()}>{busy ? 'Saving…' : 'Add metadata'}</button>
      </fieldset>
    </form>
  </section>;
}

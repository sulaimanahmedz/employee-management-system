import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { api } from '../../lib/api';

type Category = 'CONTRACT' | 'CERTIFICATE' | 'OTHER';
interface DocumentMetadata { id: number; name: string; category: Category; expiresOn: string | null }
interface DocumentPage { items: DocumentMetadata[]; page: number; totalPages: number; totalElements: number }
const categories: Record<Category, string> = { CONTRACT: 'Contract', CERTIFICATE: 'Certificate', OTHER: 'Other' };

export default function EmployeeDocuments({ employeeId }: { employeeId: number }) {
  const [data, setData] = useState<DocumentPage | null>(null);
  const [queryDraft, setQueryDraft] = useState('');
  const [query, setQuery] = useState('');
  const [categoryDraft, setCategoryDraft] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [expiryDraft, setExpiryDraft] = useState('');
  const [expiresBy, setExpiresBy] = useState('');
  const [page, setPage] = useState(0);
  const documents = data?.items ?? [];
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
    api<DocumentPage>(`${path}/page?page=${page}&q=${encodeURIComponent(query)}${expiresBy ? `&expiresBy=${encodeURIComponent(expiresBy)}` : ''}${categoryFilter ? `&category=${categoryFilter}` : ''}`, { signal: controller.signal }).then(value => {
      if (controller.signal.aborted) return;
      const last = Math.max(0, value.totalPages - 1);
      if (page > last) { setPage(last); return; }
      setData(value);
    }).catch((cause: unknown) => {
      if (!controller.signal.aborted) setError(cause instanceof Error ? cause.message : 'Unable to load documents.');
    }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [path, reload, page, expiresBy, categoryFilter, query]);

  async function add(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting.current || !name.trim()) return;
    submitting.current = true; setBusy(true); setError(''); setNotice('');
    try {
      await api<DocumentMetadata>(editing ? `${path}/${editing.id}` : path, { method: editing ? 'PUT' : 'POST',
        body: JSON.stringify({ name: name.trim(), category, expiresOn: expiresOn || null }) });
      if (!editing) setPage(0);
      setReload(value => value + 1);
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
      setReload(value => value + 1);
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
    <form aria-label="Filter document register" onSubmit={event => { event.preventDefault(); setQuery(queryDraft.trim()); setExpiresBy(expiryDraft); setCategoryFilter(categoryDraft); setPage(0); setReload(value => value + 1); }}>
      <fieldset disabled={busy || editing !== null} className="profile-fieldset"><legend>Filter document register</legend>
        <label>Search document names<input type="search" maxLength={150} value={queryDraft} onChange={event => setQueryDraft(event.target.value)} /></label>
        <label>Document category<select value={categoryDraft} onChange={event => setCategoryDraft(event.target.value)}>
          <option value="">All categories</option>{Object.entries(categories).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select></label>
        <label>Expires on or before<input type="date" value={expiryDraft} onChange={event => setExpiryDraft(event.target.value)} /></label>
        <button type="submit">Apply document filters</button>{' '}
        <button type="button" onClick={() => { setQueryDraft(''); setQuery(''); setExpiryDraft(''); setExpiresBy(''); setCategoryDraft(''); setCategoryFilter(''); setPage(0); setReload(value => value + 1); }}>Clear document filters</button>
      </fieldset>
    </form>
    {query && <p role="status">Showing document names containing: {query}. New or edited records may fall outside this search.</p>}
    {categoryFilter && <p role="status">Showing category: {categories[categoryFilter as Category]}. New or edited records may fall outside this filter.</p>}
    {expiresBy && <p role="status">Showing expiry dates on or before {expiresBy}. Records without expiry dates are excluded. New or edited records may fall outside this filter.</p>}
    {loading ? <p role="status">Loading document register…</p> : documents.length === 0 ?
      <p>{query || expiresBy || categoryFilter ? 'No document metadata matches these filters.' : 'No document metadata registered.'}</p> : <ul className="document-list">
        {documents.map(document => <li className="document-item" key={document.id}>
          <strong className="document-name">{document.name}</strong>
          <div className="document-meta">{categories[document.category]} — {document.expiresOn ?
            <>Expires <time dateTime={document.expiresOn}>{document.expiresOn}</time></> : 'No expiry date'}</div>
          <div className="document-actions">
          <button type="button" disabled={busy} onClick={() => beginEdit(document)} aria-label={`Edit ${document.name}`}>Edit</button>{' '}
          <button type="button" disabled={busy || editing !== null} onClick={() => remove(document)}
            aria-label={`Remove ${document.name}`}>Remove</button>
          </div>
        </li>)}
      </ul>}
    {!loading && data && <p>{data.totalElements} matching document records</p>}
    {!loading && data && data.totalPages > 1 && <nav aria-label="Document register pages">
      <button type="button" disabled={busy || editing !== null || page === 0} onClick={() => setPage(value => value - 1)}>Previous documents</button>
      <span> Page {page + 1} of {data.totalPages} </span>
      <button type="button" disabled={busy || editing !== null || page + 1 >= data.totalPages} onClick={() => setPage(value => value + 1)}>Next documents</button>
    </nav>}
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

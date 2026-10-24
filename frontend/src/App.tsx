import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import EmployeeProfile from './features/employees/EmployeeProfile';
import { createEmployee, searchEmployees } from './features/employees/api';
import type { EmployeeDirectory, EmployeeInput, DirectoryFilters, DirectorySort } from './features/employees/types';

const emptyForm: EmployeeInput = { firstName: '', lastName: '', email: '', department: '', jobTitle: '', status: 'ACTIVE' };

const defaultFilters: DirectoryFilters = { q: '', status: '', size: 10, sortBy: 'id', direction: 'asc' };
const sortLabels: Record<DirectorySort, string> = { id: 'Employee ID', name: 'Last name, first name', department: 'Department', jobTitle: 'Job title' };

export default function App() {
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [directory, setDirectory] = useState<EmployeeDirectory | null>(null);
  const [filters, setFilters] = useState<DirectoryFilters>(defaultFilters);
  const [draftStatus, setDraftStatus] = useState<DirectoryFilters['status']>('');
  const [draftSize, setDraftSize] = useState(10);
  const [draftSort, setDraftSort] = useState<DirectorySort>('id');
  const [draftDirection, setDraftDirection] = useState<'asc' | 'desc'>('asc');
  const [page, setPage] = useState(0);
  const [directoryError, setDirectoryError] = useState('');
  const [form, setForm] = useState<EmployeeInput>(emptyForm);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [reload, setReload] = useState(0);

  useEffect(() => {
    if (selectedId !== null) return;
    const controller = new AbortController();
    setLoading(true);
    setDirectory(null);
    setDirectoryError('');
    searchEmployees(filters, page, controller.signal)
      .then(result => {
        if (controller.signal.aborted) return;
        // A deletion or profile edit may leave the requested last page empty.
        const lastPage = Math.max(0, result.totalPages - 1);
        if (page > lastPage) {
          setPage(lastPage);
          return;
        }
        setDirectory(result);
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) {
          setDirectoryError(cause instanceof Error ? cause.message : 'Unable to load directory.');
        }
      })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [reload, filters, page, selectedId]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true); setError(''); setNotice('');
    try {
      await createEmployee(form);
      setPage(0);
      setReload(value => value + 1);
      setForm(emptyForm); setShowForm(false); setNotice('Employee added successfully.');
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to add employee.');
    } finally { setSaving(false); }
  }

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPage(0);
    setFilters({ q: query.trim(), status: draftStatus, size: draftSize, sortBy: draftSort, direction: draftDirection });
  }

  function clearFilters() {
    setQuery(''); setDraftStatus(''); setDraftSize(10); setPage(0);
    setDraftSort('id'); setDraftDirection('asc');
    setFilters(defaultFilters);
  }

  const employees = directory?.items ?? [];

  return <div className="shell">
    <aside><a className="brand" href="/">EMS<span>Employee Management</span></a><nav aria-label="Main navigation"><a href={selectedId === null ? "#directory" : "#profile-heading"} aria-current="page">{selectedId === null ? "Employee directory" : "Employee profile"}</a></nav><p className="sidebar-note">A foundation for your people operations.</p></aside>
    <main>
      {selectedId !== null ? <EmployeeProfile key={selectedId} employeeId={selectedId}
        onBack={() => setSelectedId(null)}
        onSaved={() => setReload(value => value + 1)} /> : <>
      <header><div><p className="eyebrow">PEOPLE OPERATIONS</p><h1>Your people, connected.</h1><p>Manage employee profiles and keep your directory up to date.</p></div><button disabled={loading} onClick={() => { setShowForm(value => !value); setNotice(''); }}>{showForm ? 'Close form' : '+ Add employee'}</button></header>
      <section className="stats" aria-label="Directory overview"><article><span>Total employees</span><strong>{loading || !directory ? '—' : directory.totalEmployees}</strong></article><article><span>Active employees</span><strong>{loading || !directory ? '—' : directory.activeEmployees}</strong></article><article><span>Departments</span><strong>{loading || !directory ? '—' : directory.departments}</strong></article></section>
      {error && <div className="error" role="alert">{error}</div>}
      {notice && <p className="notice" role="status">{notice}</p>}
      {showForm && <section className="panel"><h2>Add employee</h2><form onSubmit={submit}>
        <div className="form-grid">{(['firstName', 'lastName', 'email', 'department', 'jobTitle'] as const).map(field => <label key={field}>{({ firstName: 'First name', lastName: 'Last name', email: 'Work email', department: 'Department', jobTitle: 'Job title' })[field]}<input type={field === 'email' ? 'email' : 'text'} required maxLength={field === 'email' ? 254 : 100} value={form[field]} onChange={e => setForm({ ...form, [field]: e.target.value })} /></label>)}
        <label>Status<select value={form.status} onChange={e => setForm({ ...form, status: e.target.value as EmployeeInput['status'] })}><option value="ACTIVE">Active</option><option value="ON_LEAVE">On leave</option><option value="INACTIVE">Inactive</option></select></label></div>
        <button disabled={saving}>{saving ? 'Saving…' : 'Save employee'}</button>
      </form></section>}
      <section className="panel" id="directory">
        <h2>Employee directory</h2>
        <form className="directory-filters" onSubmit={applyFilters} aria-label="Filter employee directory">
          <label className="search">Search employees
            <input type="search" maxLength={200} placeholder="Name, email, department or job title"
              value={query} onChange={event => setQuery(event.target.value)} />
          </label>
          <label>Employee status
            <select value={draftStatus} onChange={event => setDraftStatus(event.target.value as DirectoryFilters['status'])}>
              <option value="">All statuses</option><option value="ACTIVE">Active</option>
              <option value="ON_LEAVE">On leave</option><option value="INACTIVE">Inactive</option>
            </select>
          </label>
          <label>Rows per page
            <select value={draftSize} onChange={event => setDraftSize(Number(event.target.value))}>
              <option value={10}>10</option><option value={25}>25</option><option value={50}>50</option>
            </select>
          </label>
          <label>Sort employees by
            <select value={draftSort} onChange={event => setDraftSort(event.target.value as DirectorySort)}>
              {Object.entries(sortLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </select>
          </label>
          <label>Sort direction
            <select value={draftDirection} onChange={event => setDraftDirection(event.target.value as 'asc' | 'desc')}>
              <option value="asc">Ascending</option><option value="desc">Descending</option>
            </select>
          </label>
          <button type="submit" disabled={saving}>Apply filters</button>
          <button type="button" disabled={saving} onClick={clearFilters}>Clear filters</button>
        </form>
        <p className="muted">Search: {filters.q || 'All employees'} · Status: {filters.status.replace('_', ' ') || 'All statuses'} · Sort: {sortLabels[filters.sortBy]} ({filters.direction === 'asc' ? 'ascending' : 'descending'}). Overview totals cover the whole organization.</p>
        {directoryError && <div className="error" role="alert">{directoryError}
          <button type="button" className="secondary" onClick={() => setReload(value => value + 1)}>Retry directory</button>
        </div>}
        {loading ? <p role="status">Loading employees…</p> : directoryError ? <p className="empty">Directory unavailable. Retry to load employees.</p> : employees.length === 0 ? <div className="empty"><h3>{directory?.totalEmployees === 0 ? 'Your directory starts here' : 'No employees match your filters'}</h3><p>{directory?.totalEmployees === 0 ? 'Add your first employee to start building your team directory.' : 'Try a different search or clear the filters.'}</p></div> : <div className="table-wrap"><table><thead><tr><th>Name</th><th>Email</th><th>Department</th><th>Job title</th><th>Status</th></tr></thead><tbody>{employees.map(employee => <tr key={employee.id}><td className="employee-name"><button type="button" className="profile-link" disabled={saving}
          aria-label={`View profile for ${employee.firstName} ${employee.lastName}`}
          onClick={() => { setShowForm(false); setNotice(''); setSelectedId(employee.id); }}>
          {employee.firstName} {employee.lastName}</button></td><td>{employee.email}</td><td>{employee.department}</td><td>{employee.jobTitle}</td><td><span className={`badge ${employee.status.toLowerCase()}`}>{employee.status.replace('_', ' ')}</span></td></tr>)}</tbody></table></div>}
        {!loading && directory && <div className="directory-pagination">
          <p role="status">{directory.totalElements === 0 ? '0 results' :
            `Showing ${directory.page * directory.size + 1}–${directory.page * directory.size + directory.items.length} of ${directory.totalElements}`}</p>
          <div className="profile-actions" role="group" aria-label="Directory pages">
            <button type="button" disabled={saving || directory.page === 0}
              onClick={() => setPage(value => Math.max(0, value - 1))}>Previous</button>
            <span>Page {directory.totalPages === 0 ? 0 : directory.page + 1} of {directory.totalPages}</span>
            <button type="button" disabled={saving || directory.page + 1 >= directory.totalPages}
              onClick={() => setPage(value => value + 1)}>Next</button>
          </div>
        </div>}
      </section>
      </>}
    </main>
  </div>;
}

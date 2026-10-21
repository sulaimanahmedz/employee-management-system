import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import EmployeeProfile from './features/employees/EmployeeProfile';
import { createEmployee, listEmployees } from './features/employees/api';
import type { Employee, EmployeeInput } from './features/employees/types';

const emptyForm: EmployeeInput = { firstName: '', lastName: '', email: '', department: '', jobTitle: '', status: 'ACTIVE' };

export default function App() {
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [form, setForm] = useState<EmployeeInput>(emptyForm);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [reload, setReload] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError('');
    listEmployees(controller.signal)
      .then(setEmployees)
      .catch((cause: Error) => { if (!controller.signal.aborted) setError(cause.message); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [reload]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true); setError(''); setNotice('');
    try {
      const employee = await createEmployee(form);
      setEmployees(current => [...current, employee]);
      setForm(emptyForm); setShowForm(false); setNotice('Employee added successfully.');
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to add employee.');
    } finally { setSaving(false); }
  }

  const filtered = employees.filter(employee =>
    `${employee.firstName} ${employee.lastName} ${employee.email} ${employee.department}`.toLowerCase().includes(query.toLowerCase()),
  );

  return <div className="shell">
    <aside><a className="brand" href="/">EMS<span>Employee Management</span></a><nav aria-label="Main navigation"><a href={selectedId === null ? "#directory" : "#profile-heading"} aria-current="page">{selectedId === null ? "Employee directory" : "Employee profile"}</a></nav><p className="sidebar-note">A foundation for your people operations.</p></aside>
    <main>
      {selectedId !== null ? <EmployeeProfile key={selectedId} employeeId={selectedId}
        onBack={() => setSelectedId(null)}
        onSaved={updated => setEmployees(current =>
          current.map(employee => employee.id === updated.id ? updated : employee))} /> : <>
      <header><div><p className="eyebrow">PEOPLE OPERATIONS</p><h1>Your people, connected.</h1><p>Manage employee profiles and keep your directory up to date.</p></div><button disabled={loading} onClick={() => { setShowForm(value => !value); setNotice(''); }}>{showForm ? 'Close form' : '+ Add employee'}</button></header>
      <section className="stats" aria-label="Directory overview"><article><span>Total employees</span><strong>{loading ? '—' : employees.length}</strong></article><article><span>Active employees</span><strong>{loading ? '—' : employees.filter(e => e.status === 'ACTIVE').length}</strong></article><article><span>Departments</span><strong>{loading ? '—' : new Set(employees.map(e => e.department)).size}</strong></article></section>
      {error && <div className="error" role="alert">{error} <button className="secondary" onClick={() => setReload(value => value + 1)}>Retry connection</button></div>}
      {notice && <p className="notice" role="status">{notice}</p>}
      {showForm && <section className="panel"><h2>Add employee</h2><form onSubmit={submit}>
        <div className="form-grid">{(['firstName', 'lastName', 'email', 'department', 'jobTitle'] as const).map(field => <label key={field}>{({ firstName: 'First name', lastName: 'Last name', email: 'Work email', department: 'Department', jobTitle: 'Job title' })[field]}<input type={field === 'email' ? 'email' : 'text'} required maxLength={field === 'email' ? 254 : 100} value={form[field]} onChange={e => setForm({ ...form, [field]: e.target.value })} /></label>)}
        <label>Status<select value={form.status} onChange={e => setForm({ ...form, status: e.target.value as EmployeeInput['status'] })}><option value="ACTIVE">Active</option><option value="ON_LEAVE">On leave</option><option value="INACTIVE">Inactive</option></select></label></div>
        <button disabled={saving}>{saving ? 'Saving…' : 'Save employee'}</button>
      </form></section>}
      <section className="panel" id="directory"><div className="section-heading"><h2>Employee directory</h2><label className="search">Search employees<input type="search" placeholder="Name, email or department" value={query} onChange={e => setQuery(e.target.value)} /></label></div>
        {loading ? <p role="status">Loading employees…</p> : employees.length === 0 ? <div className="empty"><h3>{error ? 'Directory unavailable' : 'Your directory starts here'}</h3><p>{error ? 'Restore the backend connection to load your employees.' : 'Add your first employee to start building your team directory.'}</p></div> : <div className="table-wrap"><table><thead><tr><th>Name</th><th>Email</th><th>Department</th><th>Job title</th><th>Status</th></tr></thead><tbody>{filtered.map(employee => <tr key={employee.id}><td className="employee-name"><button type="button" className="profile-link" disabled={saving}
          aria-label={`View profile for ${employee.firstName} ${employee.lastName}`}
          onClick={() => { setShowForm(false); setNotice(''); setSelectedId(employee.id); }}>
          {employee.firstName} {employee.lastName}</button></td><td>{employee.email}</td><td>{employee.department}</td><td>{employee.jobTitle}</td><td><span className={`badge ${employee.status.toLowerCase()}`}>{employee.status.replace('_', ' ')}</span></td></tr>)}</tbody></table>{filtered.length === 0 && <p className="empty">No employees match your search.</p>}</div>}
      </section>
      </>}
    </main>
  </div>;
}

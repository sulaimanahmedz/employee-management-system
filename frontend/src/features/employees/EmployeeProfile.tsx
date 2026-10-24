import { useEffect, useRef, useState } from 'react';
import EmployeeDocuments from './EmployeeDocuments';
import type { FormEvent } from 'react';
import { getEmployee, updateEmployee } from './api';
import type { Employee, EmployeeInput } from './types';

interface Props {
  employeeId: number;
  onBack: () => void;
  onSaved: (employee: Employee) => void;
}

const fields = [
  ['firstName', 'First name'], ['lastName', 'Last name'], ['email', 'Work email'],
  ['department', 'Department'], ['jobTitle', 'Job title'],
] as const;
const statusLabels = { ACTIVE: 'Active', ON_LEAVE: 'On leave', INACTIVE: 'Inactive' };

function inputFrom(employee: Employee): EmployeeInput {
  const { firstName, lastName, email, department, jobTitle, status } = employee;
  return { firstName, lastName, email, department, jobTitle, status };
}

export default function EmployeeProfile({ employeeId, onBack, onSaved }: Props) {
  const [employee, setEmployee] = useState<Employee | null>(null);
  const [draft, setDraft] = useState<EmployeeInput | null>(null);
  const [editing, setEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [reload, setReload] = useState(0);
  const submitting = useRef(false);
  const heading = useRef<HTMLHeadingElement>(null);
  const dirty = editing && employee !== null && draft !== null &&
    JSON.stringify(draft) !== JSON.stringify(inputFrom(employee));

  useEffect(() => {
    heading.current?.focus();
    const controller = new AbortController();
    setLoading(true);
    setError('');
    getEmployee(employeeId, controller.signal)
      .then(value => {
        if (controller.signal.aborted) return;
        setEmployee(value);
        setDraft(inputFrom(value));
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) {
          setError(cause instanceof Error ? cause.message : 'Unable to load employee.');
        }
      })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [employeeId, reload]);

  useEffect(() => {
    if (!dirty && !saving) return;
    const warn = (event: BeforeUnloadEvent) => {
      event.preventDefault();
      event.returnValue = '';
    };
    window.addEventListener('beforeunload', warn);
    return () => window.removeEventListener('beforeunload', warn);
  }, [dirty, saving]);

  function mayDiscard() {
    return !submitting.current && (!dirty || window.confirm('Discard your unsaved changes?'));
  }

  function cancelEdit() {
    if (!employee || !mayDiscard()) return;
    setDraft(inputFrom(employee));
    setEditing(false);
    setError('');
    setNotice('');
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!draft || submitting.current) return;
    submitting.current = true;
    setSaving(true);
    setError('');
    setNotice('');
    try {
      const updated = await updateEmployee(employeeId, draft);
      setEmployee(updated);
      setDraft(inputFrom(updated));
      setEditing(false);
      onSaved(updated);
      setNotice('Employee profile updated successfully.');
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to save employee.');
    } finally {
      submitting.current = false;
      setSaving(false);
    }
  }

  return <section aria-labelledby="profile-heading">
    <button type="button" disabled={saving} onClick={() => { if (mayDiscard()) onBack(); }}>
      Back to directory
    </button>
    <div className="panel">
      <div className="section-heading">
        <h1 id="profile-heading" ref={heading} tabIndex={-1}>
          {employee ? `${employee.firstName} ${employee.lastName}` : 'Employee profile'}
        </h1>
        {employee && !editing && <button type="button" onClick={() => {
          setDraft(inputFrom(employee)); setEditing(true); setError(''); setNotice('');
        }}>Edit profile</button>}
      </div>
      {loading && <p role="status">Loading employee profile…</p>}
      {error && <div className="error" role="alert">
        {error}
        {!employee && !loading && <button type="button" className="secondary"
          onClick={() => setReload(value => value + 1)}>Retry</button>}
      </div>}
      {notice && <p className="notice" role="status">{notice}</p>}
      {!loading && employee && !editing && <dl className="profile-details">
        <div><dt>Employee ID</dt><dd>{employee.id}</dd></div>
        {fields.map(([field, label]) => <div key={field}>
          <dt>{label}</dt><dd>{employee[field]}</dd>
        </div>)}
        <div><dt>Status</dt><dd><span className={`badge ${employee.status.toLowerCase()}`}>
          {statusLabels[employee.status]}
        </span></dd></div>
      </dl>}
      {editing && draft && <form onSubmit={save} aria-label="Edit employee profile">
        <fieldset disabled={saving} className="profile-fieldset">
          <legend className="sr-only">Employee details</legend>
          <div className="form-grid">
            {fields.map(([field, label], index) => <label key={field}>{label}
              <input autoFocus={index === 0} type={field === 'email' ? 'email' : 'text'}
                required maxLength={field === 'email' ? 254 : 100} value={draft[field]}
                onChange={event => setDraft({ ...draft, [field]: event.target.value })} />
            </label>)}
            <label>Status
              <select value={draft.status} onChange={event =>
                setDraft({ ...draft, status: event.target.value as EmployeeInput['status'] })}>
                <option value="ACTIVE">Active</option>
                <option value="ON_LEAVE">On leave</option>
                <option value="INACTIVE">Inactive</option>
              </select>
            </label>
          </div>
          <div className="profile-actions">
            <button type="submit" disabled={!dirty || saving}>{saving ? 'Saving…' : 'Save changes'}</button>
            <button type="button" onClick={cancelEdit}>Cancel</button>
          </div>
        </fieldset>
        {dirty && <p className="muted">You have unsaved changes.</p>}
      </form>}
    </div>
    {!loading && employee && !editing && <EmployeeDocuments key={employee.id} employeeId={employee.id} />}
  </section>;
}

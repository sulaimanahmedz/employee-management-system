import { useEffect, useState } from 'react';
import { api } from '../../lib/api';

type Profile = { id: number; login: string; role: string };
export default function AccountProfile() {
  const [request, setRequest] = useState(0);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  useEffect(() => {
    if (!request) return;
    let current = true;
    const controller = new AbortController();
    setLoading(true); setError(''); setProfile(null);
    void api<Profile>('/api/v1/auth/profile', { signal: controller.signal })
      .then(value => { if (current) setProfile(value); })
      .catch(() => { if (current) setError('Unable to load your account. Please retry.'); })
      .finally(() => { if (current) setLoading(false); });
    return () => { current = false; controller.abort(); };
  }, [request]);
  return <section className="auth-panel" aria-label="My account">
    <h2>My account</h2>
    <button type="button" disabled={loading} onClick={() => setRequest(value => value + 1)}>View my account</button>
    {loading && <p role="status">Loading your account…</p>}
    {error && <p role="alert">{error}</p>}
    {profile && <dl>
      <dt>Account ID</dt><dd>{profile.id}</dd>
      <dt>Login</dt><dd>{profile.login}</dd>
      <dt>Assigned role</dt><dd>{profile.role}</dd>
    </dl>}
  </section>;
}

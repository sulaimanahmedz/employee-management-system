let csrf: { headerName: string; token: string } | undefined;
export function setCsrfToken(value: typeof csrf) { csrf = value; }
const baseUrl = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');

export async function api<T>(path: string, options?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${baseUrl}${path}`, {
      ...options,
      headers: { 'Content-Type': 'application/json', ...(csrf ? { [csrf.headerName]: csrf.token } : {}), ...options?.headers },
    });
  } catch {
    throw new Error('Cannot reach the server. Check that the backend is running.');
  }
  if (response.status === 401) window.dispatchEvent(new Event('session-expired'));
  if (!response.ok) {
    const problem = await response.json().catch(() => null);
    throw new Error(problem?.detail ?? `Request failed (${response.status}). Please try again.`);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

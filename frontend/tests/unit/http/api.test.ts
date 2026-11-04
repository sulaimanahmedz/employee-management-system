import { afterEach, expect, it, vi } from 'vitest';
import { api } from '../../../src/lib/api';

afterEach(() => vi.unstubAllGlobals());

it('returns JSON and merges request headers', async () => {
  const fetcher = vi.fn().mockResolvedValue(new Response('{"id":7}', { status: 200 }));
  vi.stubGlobal('fetch', fetcher);
  expect(await api('/example', { headers: { 'X-Test': 'yes' } })).toEqual({ id: 7 });
  expect(fetcher).toHaveBeenCalledWith('/example', expect.objectContaining({
    headers: { 'Content-Type': 'application/json', 'X-Test': 'yes' },
  }));
});
it('handles no-content success', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 204 })));
  expect(await api('/example')).toBeUndefined();
});
it('reports network failures', async () => {
  vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('offline')));
  await expect(api('/example')).rejects.toThrow('Cannot reach the server');
});
it('reports problem details', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('{"detail":"Invalid task"}', { status: 400 })));
  await expect(api('/example')).rejects.toThrow('Invalid task');
});
it('handles non-JSON error responses and missing detail', async () => {
  for (const body of ['bad gateway', '{}']) {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(body, { status: 502 })));
    await expect(api('/example')).rejects.toThrow('Request failed (502)');
  }
});

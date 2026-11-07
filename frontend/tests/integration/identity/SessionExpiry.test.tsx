import { afterEach, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import AuthShell from '../../../src/features/identity/AuthShell';
import { setCsrfToken } from '../../../src/lib/api';
afterEach(() => { cleanup(); setCsrfToken(undefined); vi.unstubAllGlobals(); });
it('removes the real employee workspace when the HTTP client receives an expired-session response', async () => {
  vi.stubGlobal('fetch', vi.fn(async (url: string) => {
    if (url.endsWith('/csrf')) return Response.json({headerName:'X-CSRF-TOKEN',token:'synthetic'});
    if (url.endsWith('/session')) return Response.json({login:'synthetic-hr',authorities:['ROLE_HR']});
    return new Response(null,{status:401});
  }));
  render(<AuthShell />);
  await screen.findByRole('heading',{name:'Sign in'});
  expect(screen.getByRole('alert').textContent).toContain('session has expired');
  expect(screen.queryByRole('heading',{name:'Your people, connected.'})).toBeNull();
});

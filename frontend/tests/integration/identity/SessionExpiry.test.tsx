import { afterEach, expect, it, vi } from 'vitest';
import { act, cleanup, render, screen } from '@testing-library/react';
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

it('removes loaded employee content on a cross-tab notification without requesting the directory again', async () => {
  const fetcher = vi.fn(async (url: string) => {
    if (url.endsWith('/csrf')) return Response.json({headerName:'X-CSRF-TOKEN',token:'synthetic'});
    if (url.endsWith('/session')) return Response.json({login:'synthetic-hr',authorities:['ROLE_HR']});
    return Response.json({items:[],page:0,size:10,totalPages:0,totalElements:0,totalEmployees:0,activeEmployees:0,departments:0});
  });
  vi.stubGlobal('fetch',fetcher); render(<AuthShell />);
  await screen.findByRole('heading',{name:'Your people, connected.'});
  const before = fetcher.mock.calls.length;
  act(() => window.dispatchEvent(new StorageEvent('storage',{key:'ems-sign-out',newValue:'opaque-marker'})));
  await screen.findByRole('heading',{name:'Sign in'});
  expect(screen.queryByRole('heading',{name:'Your people, connected.'})).toBeNull();
  expect(fetcher.mock.calls.length).toBe(before);
});

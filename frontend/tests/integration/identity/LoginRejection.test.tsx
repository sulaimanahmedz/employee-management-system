import { afterEach, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AuthShell from '../../../src/features/identity/AuthShell';
import { setCsrfToken } from '../../../src/lib/api';

afterEach(() => { cleanup(); setCsrfToken(undefined); vi.unstubAllGlobals(); });
it('keeps rejected HTTP logins generic, clears passwords and permits an independent account to sign in', async () => {
  let signedIn = false;
  const fetcher = vi.fn(async (url: string, options?: RequestInit) => {
    if (url.endsWith('/csrf')) return Response.json({headerName:'X-CSRF-TOKEN',token:'synthetic'});
    if (url.endsWith('/session')) return signedIn ? Response.json({login:'other-hr',authorities:['ROLE_HR']}) : new Response(null,{status:401});
    if (url.endsWith('/login')) {
      signedIn = (options!.body as URLSearchParams).get('username') === 'other-hr';
      return new Response(null,{status:signedIn ? 204 : 401});
    }
    return Response.json({items:[],page:0,size:10,totalPages:0,totalElements:0,totalEmployees:0,activeEmployees:0,departments:0});
  });
  vi.stubGlobal('fetch',fetcher); const user=userEvent.setup(); render(<AuthShell />);
  await screen.findByRole('heading',{name:'Sign in'});
  await user.type(screen.getByLabelText('Login'),'limited-user');
  await user.type(screen.getByLabelText('Password'),'synthetic-password');
  await user.click(screen.getByRole('button',{name:'Sign in'}));
  expect((await screen.findByRole('alert')).textContent).toContain('wait a minute before retrying');
  expect((screen.getByLabelText('Password') as HTMLInputElement).value).toBe('');
  expect(screen.queryByRole('heading',{name:'Your people, connected.'})).toBeNull();
  await user.clear(screen.getByLabelText('Login')); await user.type(screen.getByLabelText('Login'),'other-hr');
  await user.type(screen.getByLabelText('Password'),'synthetic-password');
  await user.click(screen.getByRole('button',{name:'Sign in'}));
  await screen.findByRole('heading',{name:'Your people, connected.'});
  expect(fetcher).toHaveBeenCalledWith('/api/v1/auth/login',expect.objectContaining({headers:expect.objectContaining({'X-CSRF-TOKEN':'synthetic'})}));
});

it.each(['csrf', 'login'])('clears the password on %s transport failure and recovers through the real client', async (phase) => {
  let fail = false; let signedIn = false;
  vi.stubGlobal('fetch', vi.fn(async (url: string) => {
    if (fail && url.endsWith('/' + phase)) throw new TypeError('Network unavailable');
    if (url.endsWith('/csrf')) return Response.json({headerName:'X-CSRF-TOKEN',token:'synthetic-recovery'});
    if (url.endsWith('/session')) return signedIn ? Response.json({login:'synthetic-hr',authorities:['ROLE_HR']}) : new Response(null,{status:401});
    if (url.endsWith('/login')) { signedIn = true; return new Response(null,{status:204}); }
    return Response.json({items:[],page:0,size:10,totalPages:0,totalElements:0,totalEmployees:0,activeEmployees:0,departments:0});
  }));
  const user = userEvent.setup(); render(<AuthShell />);
  await screen.findByRole('heading',{name:'Sign in'});
  await user.type(screen.getByLabelText('Login'),'synthetic-hr');
  await user.type(screen.getByLabelText('Password'),'synthetic-password');
  fail = true;
  await user.click(screen.getByRole('button',{name:'Sign in'}));
  expect((await screen.findByRole('alert')).textContent).toContain('Cannot reach the server');
  expect((screen.getByLabelText('Password') as HTMLInputElement).value).toBe('');
  expect(screen.queryByRole('heading',{name:'Your people, connected.'})).toBeNull();
  fail = false;
  await user.type(screen.getByLabelText('Password'),'synthetic-password');
  await user.click(screen.getByRole('button',{name:'Sign in'}));
  await screen.findByRole('heading',{name:'Your people, connected.'});
});
it('recovers from a verification server error without disclosing its diagnostic body',async()=>{
  let available=false;let signedIn=false;
  vi.stubGlobal('fetch',vi.fn(async(url:string)=>{
    if(url.endsWith('/csrf'))return Response.json({headerName:'X-CSRF-TOKEN',token:'synthetic-audit'});
    if(url.endsWith('/session'))return signedIn?Response.json({login:'synthetic-hr',authorities:['ROLE_HR']}):new Response(null,{status:401});
    if(url.endsWith('/login')){
      if(!available)return Response.json({detail:'internal-verification-detail'},{status:500});
      signedIn=true;return new Response(null,{status:204});
    }
    return Response.json({items:[],page:0,size:10,totalPages:0,totalElements:0,totalEmployees:0,activeEmployees:0,departments:0});
  }));
  render(<AuthShell/>);const user=userEvent.setup();
  await screen.findByRole('heading',{name:'Sign in'});
  await user.type(screen.getByLabelText('Login'),'synthetic-hr');
  await user.type(screen.getByLabelText('Password'),'synthetic-secret');
  await user.click(screen.getByRole('button',{name:'Sign in'}));
  await screen.findByRole('alert');
  expect(document.body.textContent).not.toContain('internal-verification-detail');
  expect((screen.getByLabelText('Password') as HTMLInputElement).value).toBe('');
  available=true;await user.type(screen.getByLabelText('Password'),'synthetic-secret');
  await user.click(screen.getByRole('button',{name:'Sign in'}));
  await screen.findByRole('heading',{name:'Your people, connected.'});
});

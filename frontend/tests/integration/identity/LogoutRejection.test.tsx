import {afterEach,expect,it,vi} from 'vitest';
import {cleanup,render,screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AuthShell from '../../../src/features/identity/AuthShell';
import {setCsrfToken} from '../../../src/lib/api';
afterEach(()=>{cleanup();setCsrfToken(undefined);vi.unstubAllGlobals();});
it('preserves loaded account data after rejected logout and removes it after a successful retry',async()=>{
  let reject=true;
  vi.stubGlobal('fetch',vi.fn(async(url:string,options?:RequestInit)=>{
    if(url.endsWith('/csrf'))return Response.json({headerName:'X-CSRF-TOKEN',token:'synthetic-logout'});
    if(url.endsWith('/session'))return Response.json({login:'synthetic-member',authorities:['ROLE_EMPLOYEE']});
    if(url.endsWith('/profile'))return Response.json({id:4,login:'synthetic-member',role:'EMPLOYEE'});
    expect(url).toBe('/api/v1/auth/logout');
    expect(options?.headers).toEqual(expect.objectContaining({'X-CSRF-TOKEN':'synthetic-logout'}));
    return reject?new Response(null,{status:403}):new Response(null,{status:204});
  }));
  render(<AuthShell/>);const user=userEvent.setup();
  await user.click(await screen.findByRole('button',{name:'View my account'}));
  await screen.findByText('EMPLOYEE');
  await user.click(screen.getByRole('button',{name:'Sign out'}));
  expect((await screen.findByRole('alert')).textContent).toContain('Sign out failed');
  expect(screen.getByText('EMPLOYEE')).toBeTruthy();
  reject=false;await user.click(screen.getByRole('button',{name:'Sign out'}));
  await screen.findByRole('heading',{name:'Sign in'});
  expect(screen.queryByRole('region',{name:'My account'})).toBeNull();
});

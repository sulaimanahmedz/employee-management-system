import {afterEach,expect,it,vi} from 'vitest';
import {cleanup,render,screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AuthShell from '../../../src/features/identity/AuthShell';
import {setCsrfToken} from '../../../src/lib/api';
afterEach(()=>{cleanup();setCsrfToken(undefined);vi.unstubAllGlobals();});
it('loads the current account through the shared client and clears it after session expiry',async()=>{
  let expired=false;
  vi.stubGlobal('fetch',vi.fn(async(url:string)=>{
    if(url.endsWith('/csrf'))return Response.json({headerName:'X-CSRF-TOKEN',token:'synthetic'});
    if(url.endsWith('/session'))return Response.json({login:'synthetic-member',authorities:['ROLE_EMPLOYEE']});
    expect(url).toBe('/api/v1/auth/profile');
    return expired?new Response(null,{status:401}):Response.json({id:8,login:'synthetic-member',role:'EMPLOYEE'});
  }));
  render(<AuthShell/>);const user=userEvent.setup();
  await user.click(await screen.findByRole('button',{name:'View my account'}));
  await screen.findByText('EMPLOYEE');expect(screen.getByText('8')).toBeTruthy();
  expired=true;await user.click(screen.getByRole('button',{name:'View my account'}));
  await screen.findByRole('heading',{name:'Sign in'});
  expect(screen.queryByRole('region',{name:'My account'})).toBeNull();
});
it('does not expire a valid session on a forbidden response and allows retry',async()=>{
  let denied=true;
  vi.stubGlobal('fetch',vi.fn(async(url:string)=>{
    if(url.endsWith('/csrf'))return Response.json({headerName:'X-CSRF-TOKEN',token:'synthetic'});
    if(url.endsWith('/session'))return Response.json({login:'synthetic-member',authorities:['ROLE_EMPLOYEE']});
    expect(url).toBe('/api/v1/auth/profile');
    return denied?new Response(null,{status:403}):Response.json({id:8,login:'synthetic-member',role:'EMPLOYEE'});
  }));
  render(<AuthShell/>);const user=userEvent.setup();
  await user.click(await screen.findByRole('button',{name:'View my account'}));
  expect((await screen.findByRole('alert')).textContent).toBe('Unable to load your account. Please retry.');
  expect(screen.queryByRole('heading',{name:'Sign in'})).toBeNull();
  denied=false;await user.click(screen.getByRole('button',{name:'View my account'}));
  await screen.findByText('EMPLOYEE');expect(screen.queryByRole('alert')).toBeNull();
});

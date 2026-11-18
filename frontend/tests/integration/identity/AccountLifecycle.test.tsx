import {afterEach,expect,it,vi} from 'vitest';
import {cleanup,render,screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AuthShell from '../../../src/features/identity/AuthShell';
import {setCsrfToken} from '../../../src/lib/api';
afterEach(()=>{cleanup();setCsrfToken(undefined);vi.unstubAllGlobals();});
it('changes the current password through CSRF client then removes authenticated state',async()=>{
 vi.stubGlobal('fetch',vi.fn(async(url:string,options?:RequestInit)=>{
  if(url.endsWith('/csrf'))return Response.json({headerName:'X-CSRF-TOKEN',token:'synthetic-csrf'});
  if(url.endsWith('/session'))return Response.json({login:'synthetic-user',authorities:['ROLE_EMPLOYEE']});
  expect(url).toBe('/api/v1/auth/password');expect(options?.headers).toEqual(expect.objectContaining({'X-CSRF-TOKEN':'synthetic-csrf'}));
  expect(JSON.parse(String(options?.body))).toEqual({currentPassword:'Synthetic current!',newPassword:'Synthetic replacement!'});
  return new Response(null,{status:204});
 }));
 render(<AuthShell/>);const user=userEvent.setup();
 await user.type(await screen.findByLabelText('Current password'),'Synthetic current!');await user.type(screen.getByLabelText('New password'),'Synthetic replacement!');await user.click(screen.getByRole('button',{name:'Change password'}));
 await screen.findByRole('heading',{name:'Sign in'});expect(screen.queryByRole('region',{name:'Account security'})).toBeNull();
});
it('clears scoped employee data when a revoked session returns 401',async()=>{
 let revoked=false;
 vi.stubGlobal('fetch',vi.fn(async(url:string)=>{
  if(url.endsWith('/csrf'))return Response.json({headerName:'X-CSRF-TOKEN',token:'synthetic'});
  if(url.endsWith('/session'))return Response.json({login:'synthetic-manager',authorities:['ROLE_MANAGER']});
  if(revoked)return new Response(null,{status:401});
  if(url.endsWith('/employees'))return Response.json([12]);
  return Response.json({id:12,firstName:'Synthetic',lastName:'Person',email:'synthetic@example.com',department:'QA',jobTitle:'Analyst',status:'ACTIVE'});
 }));
 render(<AuthShell/>);const user=userEvent.setup();await user.click(await screen.findByRole('button',{name:'View assigned employees'}));await user.click(await screen.findByRole('button',{name:'View employee 12'}));await screen.findByText('synthetic@example.com');
 revoked=true;await user.click(screen.getByRole('button',{name:'View employee 12'}));await screen.findByRole('heading',{name:'Sign in'});expect(screen.queryByText('synthetic@example.com')).toBeNull();
});

import {afterEach,expect,it,vi} from 'vitest';
import {cleanup,render,screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AuthShell from '../../../src/features/identity/AuthShell';
import {setCsrfToken} from '../../../src/lib/api';
afterEach(()=>{cleanup();setCsrfToken(undefined);vi.unstubAllGlobals();});
it('reads audit through the real client and clears it on session expiry',async()=>{
 let expired=false;
 vi.stubGlobal('fetch',vi.fn(async(url:string)=>{
  if(url.endsWith('/csrf'))return Response.json({headerName:'X-CSRF-TOKEN',token:'synthetic'});
  if(url.endsWith('/session'))return Response.json({login:'synthetic-admin',authorities:['ROLE_ADMIN']});
  expect(url).toBe('/api/v1/admin/audit?page=0');
  return expired?new Response(null,{status:401}):Response.json([{id:7,occurredAt:'2026-11-19',action:'ACCOUNT_ACCESS_CHANGED',actorId:1,subjectId:2}]);
 }));
 render(<AuthShell/>);const user=userEvent.setup();await user.click(await screen.findByRole('button',{name:'View recent security events'}));await screen.findByText(/Event 7:/);
 expired=true;await user.click(screen.getByRole('button',{name:'View recent security events'}));await screen.findByRole('heading',{name:'Sign in'});expect(screen.queryByText(/Event 7:/)).toBeNull();
});
it('sends the selected action through the real HTTP client and retains it after a failed read',async()=>{
 let failed=true;const urls:string[]=[];
 vi.stubGlobal('fetch',vi.fn(async(url:string)=>{
  if(url.endsWith('/csrf'))return Response.json({headerName:'X-CSRF-TOKEN',token:'synthetic'});
  if(url.endsWith('/session'))return Response.json({login:'synthetic-admin',authorities:['ROLE_ADMIN']});
  urls.push(url);return failed?Response.json({detail:'unavailable'},{status:503}):Response.json([]);
 }));render(<AuthShell/>);const user=userEvent.setup();await user.selectOptions(await screen.findByLabelText('Security event action'),'ACCESS_DENIED');await screen.findByRole('alert');failed=false;
 await user.click(screen.getByRole('button',{name:'Retry security events'}));await screen.findByText('No security events on this page.');expect(urls).toEqual(['/api/v1/admin/audit?page=0&action=ACCESS_DENIED','/api/v1/admin/audit?page=0&action=ACCESS_DENIED']);
});

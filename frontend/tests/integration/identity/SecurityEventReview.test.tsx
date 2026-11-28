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
it('retains the applied subject through a failed read and retry',async()=>{
 let failed=false;const urls:string[]=[];
 vi.stubGlobal('fetch',vi.fn(async(url:string)=>{
  if(url.endsWith('/csrf'))return Response.json({headerName:'X-CSRF-TOKEN',token:'synthetic'});
  if(url.endsWith('/session'))return Response.json({login:'synthetic-admin',authorities:['ROLE_ADMIN']});
  urls.push(url);return failed?new Response(null,{status:503}):Response.json([{id:7,occurredAt:'2026-11-27',action:'PASSWORD_RESET',actorId:1,subjectId:9}]);
 }));render(<AuthShell/>);const user=userEvent.setup();
 await user.type(await screen.findByLabelText('Subject account ID'),'9');await user.click(screen.getByRole('button',{name:'Apply subject filter'}));await screen.findByText(/Event 7:/);
 failed=true;await user.click(screen.getByRole('button',{name:'View recent security events'}));await screen.findByRole('alert');expect(screen.queryByText(/Event 7:/)).toBeNull();
 await user.clear(screen.getByLabelText('Subject account ID'));await user.type(screen.getByLabelText('Subject account ID'),'10');
 failed=false;await user.click(screen.getByRole('button',{name:'Retry security events'}));await screen.findByText(/Event 7:/);
 expect(urls).toEqual(Array(3).fill('/api/v1/admin/audit?page=0&subjectId=9'));
 expect(screen.getByText(/Subject filter: 9\./)).toBeTruthy();
});
it('preserves applied actor on failed refresh and ignores unsubmitted edits during retry',async()=>{
 let failed=false;const urls:string[]=[];
 vi.stubGlobal('fetch',vi.fn(async(url:string)=>{
  if(url.endsWith('/csrf'))return Response.json({headerName:'X-CSRF-TOKEN',token:'synthetic'});
  if(url.endsWith('/session'))return Response.json({login:'synthetic-admin',authorities:['ROLE_ADMIN']});
  urls.push(url);return failed?new Response(null,{status:503}):Response.json([{id:7,occurredAt:'2026-11-28',action:'PASSWORD_RESET',actorId:7,subjectId:9}]);
 }));render(<AuthShell/>);const user=userEvent.setup();
 await user.type(await screen.findByLabelText('Actor account ID'),'7');await user.click(screen.getByRole('button',{name:'Apply actor filter'}));await screen.findByText(/Event 7:/);
 failed=true;await user.click(screen.getByRole('button',{name:'View recent security events'}));await screen.findByRole('alert');expect(screen.queryByText(/Event 7:/)).toBeNull();
 await user.clear(screen.getByLabelText('Actor account ID'));await user.type(screen.getByLabelText('Actor account ID'),'8');failed=false;
 await user.click(screen.getByRole('button',{name:'Retry security events'}));await screen.findByText(/Event 7:/);
 expect(urls).toEqual(Array(3).fill('/api/v1/admin/audit?page=0&actorId=7'));expect(screen.getByText(/Actor filter: 7\./)).toBeTruthy();
});

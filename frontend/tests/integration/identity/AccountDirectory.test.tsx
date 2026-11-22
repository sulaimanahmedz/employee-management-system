import { afterEach, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AuthShell from '../../../src/features/identity/AuthShell';
import { setCsrfToken } from '../../../src/lib/api';
afterEach(()=>{cleanup();setCsrfToken(undefined);vi.unstubAllGlobals();});
it('uses the shared authenticated client and removes the directory after a 401',async()=>{
  let expired=false;
  vi.stubGlobal('fetch',vi.fn(async(url:string,options?:RequestInit)=>{
    if(url.endsWith('/csrf'))return Response.json({headerName:'X-CSRF-TOKEN',token:'synthetic-csrf'});
    if(url.endsWith('/session'))return Response.json({login:'synthetic-admin',authorities:['ROLE_ADMIN']});
    expect(url).toBe('/api/v1/admin/accounts?page=0');
    expect(options?.headers).toEqual(expect.objectContaining({'X-CSRF-TOKEN':'synthetic-csrf'}));
    return expired ? new Response(null,{status:401}) : Response.json({items:[{id:1,login:'synthetic-visible',role:'HR'}],page:0,totalPages:1,totalElements:1});
  }));
  render(<AuthShell/>);const user=userEvent.setup();
  await user.click(await screen.findByRole('button',{name:'View accounts'}));
  await screen.findByText('synthetic-visible');
  expired=true;await user.click(screen.getByRole('button',{name:'View accounts'}));
  await screen.findByRole('heading',{name:'Sign in'});
  expect(screen.queryByText('synthetic-visible')).toBeNull();
  expect(screen.queryByRole('region',{name:'Account directory'})).toBeNull();
});

it('retains the role query through failed reads and refresh recovery',async()=>{
  let failed=true;const queries:string[]=[];
  vi.stubGlobal('fetch',vi.fn(async(url:string)=>{
    if(url.endsWith('/csrf'))return Response.json({headerName:'X-CSRF-TOKEN',token:'synthetic-csrf'});
    if(url.endsWith('/session'))return Response.json({login:'synthetic-admin',authorities:['ROLE_ADMIN']});
    queries.push(url);
    return failed ? new Response(null,{status:503}) : Response.json({items:[],page:0,totalPages:0,totalElements:0});
  }));
  render(<AuthShell/>);const user=userEvent.setup();
  await user.selectOptions(await screen.findByLabelText('Filter accounts by role'),'MANAGER');
  await screen.findByText('Unable to load accounts. Please retry.');
  failed=false;await user.click(screen.getByRole('button',{name:'View accounts'}));
  await screen.findByText('0 accounts · Page 1 of 1');
  expect(queries).toEqual(['/api/v1/admin/accounts?page=0&role=MANAGER','/api/v1/admin/accounts?page=0&role=MANAGER']);
  expect((screen.getByLabelText('Filter accounts by role') as HTMLSelectElement).value).toBe('MANAGER');
});

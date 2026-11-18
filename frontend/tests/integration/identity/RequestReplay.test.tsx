import {afterEach,expect,it,vi} from 'vitest';
import {cleanup,render,screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AuthShell from '../../../src/features/identity/AuthShell';
import {api,setCsrfToken} from '../../../src/lib/api';
afterEach(()=>{cleanup();setCsrfToken(undefined);vi.unstubAllGlobals();});
it('does not replay a rejected request after explicit sign-in',async()=>{
  let signedIn=false;let protectedReads=0;
  vi.stubGlobal('fetch',vi.fn(async(url:string)=>{
    if(url==='/api/v1/employees?search=synthetic-private-value') { protectedReads++;return new Response(null,{status:401}); }
    if(url.endsWith('/csrf'))return Response.json({headerName:'X-CSRF-TOKEN',token:'synthetic'});
    if(url.endsWith('/login')){signedIn=true;return new Response(null,{status:204});}
    expect(url).toBe('/api/v1/auth/session');
    return signedIn?Response.json({login:'synthetic-member',authorities:['ROLE_EMPLOYEE']}):new Response(null,{status:401});
  }));
  await expect(api('/api/v1/employees?search=synthetic-private-value')).rejects.toThrow('401');
  render(<AuthShell/>);const user=userEvent.setup();
  await screen.findByRole('heading',{name:'Sign in'});
  await user.type(screen.getByLabelText('Login'),'synthetic-member');
  await user.type(screen.getByLabelText('Password'),'synthetic-password');
  await user.click(screen.getByRole('button',{name:'Sign in'}));
  await screen.findByRole('heading',{name:'Access denied'});
  expect(protectedReads).toBe(1);
});

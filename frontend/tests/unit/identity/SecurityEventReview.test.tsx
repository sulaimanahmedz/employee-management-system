import { afterEach, expect, it, vi } from 'vitest';
import { act, cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SecurityEventReview from '../../../src/features/identity/SecurityEventReview';
import { api } from '../../../src/lib/api';
vi.mock('../../../src/lib/api', () => ({ api: vi.fn() }));
afterEach(() => { cleanup(); vi.resetAllMocks(); });
const events=Array.from({length:20},(_,i)=>({id:40-i,occurredAt:'2026-11-19T00:00:00Z',action:'ACCOUNT_ACCESS_CHANGED',actorId:i===0?null:1,subjectId:i===0?null:2}));
it('loads on demand, navigates older/newer pages, identifies empty pages and refreshes',async()=>{
 vi.mocked(api).mockResolvedValueOnce(events).mockResolvedValueOnce([]).mockResolvedValueOnce(events).mockResolvedValueOnce(events.slice(0,1));
 render(<SecurityEventReview/>);const user=userEvent.setup();expect(api).not.toHaveBeenCalled();
 await user.click(screen.getByRole('button',{name:'View recent security events'}));
 await screen.findByText(/Event 40:/);expect(screen.getByText(/actor unattributed — subject unattributed/)).toBeTruthy();
 expect((screen.getByRole('button',{name:'Newer security events'}) as HTMLButtonElement).disabled).toBe(true);
 expect(screen.getAllByRole('listitem')).toHaveLength(20);
 await user.click(screen.getByRole('button',{name:'Older security events'}));await screen.findByText('No security events on this page.');
 expect(screen.getByRole('status').textContent).toContain('Page 2');expect((screen.getByRole('button',{name:'Older security events'}) as HTMLButtonElement).disabled).toBe(true);
 await user.click(screen.getByRole('button',{name:'Newer security events'}));await screen.findByText(/Event 40:/);
 await user.click(screen.getByRole('button',{name:'View recent security events'}));await screen.findByText(/Event 40:/);expect(screen.getAllByRole('listitem')).toHaveLength(1);
 expect(vi.mocked(api).mock.calls.map(c=>c[0])).toEqual([0,1,0,0].map(p=>`/api/v1/admin/audit?page=${p}`));
});
it('clears stale records, disables loading and retries the failed page',async()=>{
 let reject!:(e:Error)=>void;
 vi.mocked(api).mockResolvedValueOnce(events).mockReturnValueOnce(new Promise((_yes,no)=>{reject=no;})).mockResolvedValueOnce(events.slice(0,1));
 render(<SecurityEventReview/>);const user=userEvent.setup();await user.click(screen.getByRole('button',{name:'View recent security events'}));await screen.findByText(/Event 40:/);
 await user.click(screen.getByRole('button',{name:'Older security events'}));expect(screen.queryByRole('listitem')).toBeNull();
 expect(screen.getByRole('status').textContent).toBe('Loading security events…');expect((screen.getByRole('button',{name:'View recent security events'}) as HTMLButtonElement).disabled).toBe(true);
 await act(async()=>reject(new Error('network')));expect(screen.getByRole('alert').textContent).toContain('Unable to load');
 await user.click(screen.getByRole('button',{name:'Retry security events'}));await screen.findByText(/Event 40:/);expect(screen.getByRole('status').textContent).toContain('Page 2');expect(screen.queryByRole('alert')).toBeNull();
 expect(vi.mocked(api).mock.calls[2][0]).toBe('/api/v1/admin/audit?page=1');
});
it.each([true,false])('aborts on unmount and ignores late completion (success=%s)',async success=>{
 let resolve!:(v:unknown)=>void;let reject!:(e:Error)=>void;
 vi.mocked(api).mockReturnValue(new Promise((yes,no)=>{resolve=yes;reject=no;}));
 const view=render(<SecurityEventReview/>);await userEvent.setup().click(screen.getByRole('button',{name:'View recent security events'}));
 const signal=vi.mocked(api).mock.calls[0][1]!.signal!;view.unmount();expect(signal.aborted).toBe(true);
 await act(async()=>{if(success)resolve(events);else reject(new Error('aborted'));});expect(screen.queryByRole('listitem')).toBeNull();expect(screen.queryByRole('alert')).toBeNull();
});
it('applies an action at page zero, retains it through paging and retry, then clears it',async()=>{
 const user=userEvent.setup();vi.mocked(api).mockResolvedValue(events);render(<SecurityEventReview/>);
 await user.click(screen.getByRole('button',{name:'View recent security events'}));await screen.findByText(/Event 40:/);
 await user.click(screen.getByRole('button',{name:'Older security events'}));await screen.findByText('Security events · Page 2');
 await user.selectOptions(screen.getByLabelText('Security event action'),'PASSWORD_RESET');await screen.findByText('Security events · Page 1');
 expect(vi.mocked(api).mock.lastCall![0]).toBe('/api/v1/admin/audit?page=0&action=PASSWORD_RESET');
 vi.mocked(api).mockRejectedValueOnce(new Error('offline'));await user.click(screen.getByRole('button',{name:'Older security events'}));await screen.findByRole('alert');
 await user.click(screen.getByRole('button',{name:'Retry security events'}));await screen.findByText('Security events · Page 2');expect(vi.mocked(api).mock.lastCall![0]).toBe('/api/v1/admin/audit?page=1&action=PASSWORD_RESET');
 await user.click(screen.getByRole('button',{name:'View recent security events'}));await screen.findByText('Security events · Page 1');expect(vi.mocked(api).mock.lastCall![0]).toBe('/api/v1/admin/audit?page=0&action=PASSWORD_RESET');
 await user.selectOptions(screen.getByLabelText('Security event action'),'');await screen.findByText(/Event 40:/);expect(vi.mocked(api).mock.lastCall![0]).toBe('/api/v1/admin/audit?page=0');
});

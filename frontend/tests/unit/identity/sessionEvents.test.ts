import { afterEach, expect, it, vi } from 'vitest';
import { announceSignOut, listenForSignOut } from '../../../src/features/identity/sessionEvents';
afterEach(() => { localStorage.clear(); vi.restoreAllMocks(); });
it('publishes unique opaque markers without storing credentials', () => {
  announceSignOut(); const first = localStorage.getItem('ems-sign-out');
  announceSignOut(); const second = localStorage.getItem('ems-sign-out');
  expect(first).toMatch(/^[0-9a-f-]{36}$/); expect(second).not.toBe(first);
  expect(localStorage.length).toBe(1);
});
it('does not turn successful server logout into failure when storage is unavailable', () => {
  vi.spyOn(Storage.prototype,'setItem').mockImplementation(() => { throw new DOMException('Denied','SecurityError'); });
  expect(() => announceSignOut()).not.toThrow();
});
it('ignores unrelated changes and removals and detaches its listener', () => {
  const ended = vi.fn(); const stop = listenForSignOut(ended);
  window.dispatchEvent(new StorageEvent('storage',{key:'unrelated',newValue:'marker'}));
  window.dispatchEvent(new StorageEvent('storage',{key:'ems-sign-out',newValue:null}));
  expect(ended).not.toHaveBeenCalled();
  window.dispatchEvent(new StorageEvent('storage',{key:'ems-sign-out',newValue:'marker'}));
  expect(ended).toHaveBeenCalledTimes(1); stop();
  window.dispatchEvent(new StorageEvent('storage',{key:'ems-sign-out',newValue:'later'}));
  expect(ended).toHaveBeenCalledTimes(1);
});

const signOutKey = 'ems-sign-out';

export function announceSignOut() {
  try {
    // Only a random notification marker, never identity, credentials or session tokens.
    localStorage.setItem(signOutKey, crypto.randomUUID());
  } catch {
    // Storage may be disabled; server logout still succeeds and API 401 recovery remains.
  }
}

export function listenForSignOut(onSignOut: () => void) {
  const receive = (event: StorageEvent) => {
    if (event.key === signOutKey && event.newValue !== null) onSignOut();
  };
  window.addEventListener('storage', receive);
  return () => window.removeEventListener('storage', receive);
}

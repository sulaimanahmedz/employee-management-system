package com.ems.identity.service;

import java.time.Clock;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

/** Single-instance admission guard. No passwords, tokens or request headers are retained. */
@Service
public class LoginAttemptService {
    private record Attempt(int count, long expiresAt) {}
    private final Map<String, Attempt> attempts = new HashMap<>();
    private final Clock clock;
    private int inFlight;

    public LoginAttemptService() { this(Clock.systemUTC()); }
    public LoginAttemptService(Clock clock) { this.clock = clock; }

    public synchronized void acquire(String login) {
        long now = clock.millis();
        attempts.values().removeIf(attempt -> attempt.expiresAt() <= now);
        String key = key(login);
        Attempt previous = attempts.get(key);
        if (inFlight >= 16 || (previous == null && attempts.size() >= 1024)
                || (previous != null && previous.count() >= 5)) {
            throw new BadCredentialsException("Invalid login or password");
        }
        attempts.put(key, new Attempt(previous == null ? 1 : previous.count() + 1,
            previous == null ? now + 60_000 : previous.expiresAt()));
        inFlight++;
    }

    public synchronized void succeeded(String login) { attempts.remove(key(login)); }
    public synchronized void release() { inFlight--; }

    private static String key(String login) {
        String normalized = login == null ? "" : login.strip().toLowerCase(Locale.ROOT);
        // Invalid unbounded identifiers share one bucket rather than retaining attacker-sized values.
        return normalized.length() > 64 ? "<invalid-login>" : normalized;
    }
}

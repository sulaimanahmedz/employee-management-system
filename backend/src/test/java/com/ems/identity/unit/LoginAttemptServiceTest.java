package com.ems.identity.unit;

import com.ems.identity.service.LoginAttemptService;
import com.ems.identity.service.AccountService;
import com.ems.identity.service.AccountAuthenticationProvider;
import com.ems.identity.service.AuthenticationAuditService;
import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginAttemptServiceTest {
    @Test void normalizesLoginsAndExpiresWithoutExtendingTheWindowOnRejection() {
        Clock clock = mock(Clock.class); when(clock.millis()).thenReturn(1000L);
        var guard = new LoginAttemptService(clock);
        for (int i = 0; i < 5; i++) { guard.acquire(" Synthetic.User "); guard.release(); }
        when(clock.millis()).thenReturn(60_999L);
        assertThrows(BadCredentialsException.class, () -> guard.acquire("SYNTHETIC.USER"));
        when(clock.millis()).thenReturn(61_000L);
        guard.acquire("synthetic.user"); guard.release();
        guard.succeeded("SYNTHETIC.USER");
        for (int i = 0; i < 5; i++) { guard.acquire("synthetic.user"); guard.release(); }
    }
    @Test void capsConcurrentChecksAndReleasesCapacity() {
        var guard = new LoginAttemptService();
        for (int i = 0; i < 16; i++) guard.acquire("synthetic-" + i);
        assertThrows(BadCredentialsException.class, () -> guard.acquire("another-user"));
        guard.release(); guard.acquire("another-user");
        for (int i = 0; i < 16; i++) guard.release();
    }
    @Test void boundsIdentityStorageAndReclaimsExpiredBuckets() {
        Clock clock = mock(Clock.class); when(clock.millis()).thenReturn(0L);
        var guard = new LoginAttemptService(clock);
        for (int i = 0; i < 1024; i++) { guard.acquire("synthetic-" + i); guard.release(); }
        assertThrows(BadCredentialsException.class, () -> guard.acquire("new-user"));
        guard.acquire("synthetic-0"); guard.release();
        guard.succeeded("synthetic-0"); guard.acquire("new-user"); guard.release();
        when(clock.millis()).thenReturn(60_000L);
        guard.acquire("after-expiry"); guard.release();
    }
    @Test void invalidLongIdentifiersShareABoundedBucketAndNullIsSafe() {
        var guard = new LoginAttemptService();
        guard.acquire(null); guard.release();
        for (int i = 0; i < 5; i++) { guard.acquire("x".repeat(65 + i)); guard.release(); }
        assertThrows(BadCredentialsException.class, () -> guard.acquire("y".repeat(1000)));
    }
    @Test void parallelAttemptsCannotRacePastThePerLoginLimit() throws Exception {
        var guard = new LoginAttemptService();
        var pool = Executors.newFixedThreadPool(12);
        try {
            var calls = new ArrayList<Callable<Boolean>>();
            for (int i = 0; i < 12; i++) calls.add(() -> {
                try { guard.acquire("same-login"); guard.release(); return true; }
                catch (BadCredentialsException rejected) { return false; }
            });
            int admitted = 0;
            for (var result : pool.invokeAll(calls)) if (result.get()) admitted++;
            assertEquals(5, admitted);
        } finally { pool.shutdownNow(); }
    }
    @Test void providerSkipsHashWorkWhenLimitedAndReleasesOnUnexpectedFailure() {
        var accounts = mock(AccountService.class);
        var guard = new LoginAttemptService();
        var provider = new AccountAuthenticationProvider(accounts, guard, new AuthenticationAuditService());
        when(accounts.verifyCredentials("missing", "wrong")).thenReturn(Optional.empty());
        for (int i = 0; i < 6; i++) assertThrows(BadCredentialsException.class,
            () -> provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("missing", "wrong")));
        verify(accounts, times(5)).verifyCredentials("missing", "wrong");
        when(accounts.verifyCredentials("broken", "wrong")).thenThrow(new IllegalStateException("database unavailable"));
        assertThrows(IllegalStateException.class,
            () -> provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("broken", "wrong")));
        for (int i = 0; i < 16; i++) guard.acquire("capacity-" + i);
        for (int i = 0; i < 16; i++) guard.release();
    }
}

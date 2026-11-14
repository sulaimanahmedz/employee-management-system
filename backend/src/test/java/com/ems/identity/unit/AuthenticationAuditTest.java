package com.ems.identity.unit;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ems.identity.dto.AccountIdentity;
import com.ems.identity.entity.AccountRole;
import com.ems.identity.service.AccountAuthenticationProvider;
import com.ems.identity.service.AccountService;
import com.ems.identity.service.AuthenticationAuditService;
import com.ems.identity.service.LoginAttemptService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationAuditTest {
    @Test void providerRecordsEachOutcomeOnceWithoutChangingAdmissionOrFailureSemantics() {
        var accounts = mock(AccountService.class);
        var attempts = mock(LoginAttemptService.class);
        var audit = mock(AuthenticationAuditService.class);
        var provider = new AccountAuthenticationProvider(accounts, attempts, audit);
        var input = UsernamePasswordAuthenticationToken.unauthenticated("untrusted\nlogin", "synthetic-secret");
        when(accounts.verifyCredentials(input.getName(), "synthetic-secret"))
            .thenReturn(Optional.of(new AccountIdentity(42L, "canonical", AccountRole.HR)));
        assertNull(provider.authenticate(input).getCredentials());
        verify(audit).accepted(42L); verify(attempts).release();
        clearInvocations(audit, attempts);
        when(accounts.verifyCredentials(input.getName(), "synthetic-secret")).thenReturn(Optional.empty());
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(input));
        verify(audit).rejected(); verify(attempts).release(); verifyNoMoreInteractions(audit);
        clearInvocations(audit, attempts, accounts);
        var limited = new BadCredentialsException("sensitive limiter detail");
        doThrow(limited).when(attempts).acquire(input.getName());
        assertSame(limited, assertThrows(BadCredentialsException.class, () -> provider.authenticate(input)));
        verify(audit).rejected(); verify(attempts, never()).release(); verifyNoInteractions(accounts);
        reset(attempts); clearInvocations(audit);
        var unavailable = new IllegalStateException("sensitive database detail");
        when(accounts.verifyCredentials(input.getName(), "synthetic-secret")).thenThrow(unavailable);
        assertSame(unavailable, assertThrows(IllegalStateException.class, () -> provider.authenticate(input)));
        verify(audit).unavailable(); verify(attempts).release(); verifyNoMoreInteractions(audit);
    }

    @Test void auditPayloadHasOnlyFixedEventCodesAndNumericIdentityWithoutThrowable() {
        Logger logger = (Logger) LoggerFactory.getLogger(AuthenticationAuditService.class);
        var appender = new ListAppender<ILoggingEvent>(); appender.start(); logger.addAppender(appender);
        try {
            var audit = new AuthenticationAuditService();
            audit.accepted(42L); audit.rejected(); audit.unavailable();
            assertEquals(java.util.List.of("event=AUTH_CREDENTIALS_ACCEPTED account_id=42",
                "event=AUTH_CREDENTIALS_REJECTED", "event=AUTH_VERIFICATION_UNAVAILABLE"),
                appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList());
            assertEquals(java.util.List.of(Level.INFO, Level.INFO, Level.ERROR), appender.list.stream().map(ILoggingEvent::getLevel).toList());
            appender.list.forEach(event -> { assertNull(event.getThrowableProxy()); assertTrue(event.getTimeStamp() > 0); });
        } finally { logger.detachAppender(appender); appender.stop(); }
    }
}

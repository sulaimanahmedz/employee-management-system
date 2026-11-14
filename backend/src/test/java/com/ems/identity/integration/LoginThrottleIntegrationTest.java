package com.ems.identity.integration;

import com.ems.identity.service.AccountService;
import com.ems.identity.service.PasswordHashService;
import com.ems.identity.service.LoginAttemptService;
import com.ems.identity.service.AccountAuthenticationProvider;
import com.ems.identity.service.AuthenticationAuditService;
import com.ems.identity.entity.AccountRole;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest
@Import({AccountService.class, PasswordHashService.class})
class LoginThrottleIntegrationTest {
    @Autowired AccountService accounts;
    @Test void persistedAccountRecoversAfterWindowExpiryWithoutChangingItsPassword() {
        var identity = accounts.provision("synthetic-throttle", "Synthetic integration password!", AccountRole.HR);
        Clock clock = mock(Clock.class); when(clock.millis()).thenReturn(1000L);
        var audit = spy(new AuthenticationAuditService());
        var provider = new AccountAuthenticationProvider(accounts, new LoginAttemptService(clock), audit);
        for (int i = 0; i < 5; i++) assertThrows(BadCredentialsException.class,
            () -> provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("synthetic-throttle", "wrong")));
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated("SYNTHETIC-THROTTLE", "Synthetic integration password!")));
        when(clock.millis()).thenReturn(61_000L);
        var authenticated = provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("synthetic-throttle", "Synthetic integration password!"));
        verify(audit, times(6)).rejected();
        verify(audit).accepted(identity.id());
        verify(audit, never()).unavailable();
        assertEquals("synthetic-throttle", authenticated.getName());
        assertTrue(authenticated.isAuthenticated()); assertNull(authenticated.getCredentials());
    }
}

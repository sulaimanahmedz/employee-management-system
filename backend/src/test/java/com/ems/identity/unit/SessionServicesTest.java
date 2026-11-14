package com.ems.identity.unit;

import com.ems.identity.dto.AccountIdentity;
import com.ems.identity.entity.AccountRole;
import com.ems.identity.repository.AccountRepository;
import com.ems.identity.service.AccountService;
import com.ems.identity.service.LoginAttemptService;
import com.ems.identity.service.AccountBootstrapService;
import com.ems.identity.service.AccountAuthenticationProvider;
import com.ems.identity.service.AuthenticationAuditService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionServicesTest {
    @Test void providerReturnsNoCredentialsAndRejectsInvalidCredentials() {
        var accounts = mock(AccountService.class);
        var provider = new AccountAuthenticationProvider(accounts, new LoginAttemptService(), new AuthenticationAuditService());
        assertTrue(provider.supports(UsernamePasswordAuthenticationToken.class));
        assertFalse(provider.supports(TestingAuthenticationToken.class));
        when(accounts.verifyCredentials("synthetic", "password")).thenReturn(Optional.of(new AccountIdentity(1L, "synthetic", AccountRole.HR)));
        var result = provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("synthetic", "password"));
        assertTrue(result.isAuthenticated()); assertNull(result.getCredentials());
        assertEquals("ROLE_HR", result.getAuthorities().iterator().next().getAuthority());
        when(accounts.verifyCredentials("synthetic", "bad")).thenReturn(Optional.empty());
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("synthetic", "bad")));
    }
    @Test void bootstrapRequiresExplicitCredentialsAndAnEmptyStore() throws Exception {
        var accounts = mock(AccountService.class); var repository = mock(AccountRepository.class);
        new AccountBootstrapService(accounts, repository, "", "").run(null);
        verifyNoInteractions(accounts, repository);
        assertThrows(IllegalStateException.class, () -> new AccountBootstrapService(accounts, repository, "operator", "").run(null));
        assertThrows(IllegalStateException.class, () -> new AccountBootstrapService(accounts, repository, "", "secret").run(null));
        new AccountBootstrapService(accounts, repository, "operator", "synthetic secret").run(null);
        verify(accounts).provision("operator", "synthetic secret", AccountRole.ADMIN);
        clearInvocations(accounts);
        when(repository.count()).thenReturn(1L);
        new AccountBootstrapService(accounts, repository, "operator", "synthetic secret").run(null);
        verifyNoInteractions(accounts);
    }
}

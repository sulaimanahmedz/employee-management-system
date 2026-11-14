package com.ems.identity.service;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AccountAuthenticationProvider implements AuthenticationProvider {
    private final AccountService accounts;
    private final LoginAttemptService attempts;
    private final AuthenticationAuditService audit;
    public AccountAuthenticationProvider(AccountService accounts, LoginAttemptService attempts, AuthenticationAuditService audit) {
        this.accounts = accounts; this.attempts = attempts; this.audit = audit;
    }
    @Override public Authentication authenticate(Authentication authentication) {
        String login = authentication.getName();
        try {
            attempts.acquire(login);
            try {
                var identity = accounts.verifyCredentials(login, (String) authentication.getCredentials())
                    .orElseThrow(() -> new BadCredentialsException("Invalid login or password"));
                attempts.succeeded(login);
                audit.accepted(identity.id());
                return UsernamePasswordAuthenticationToken.authenticated(identity.login(), null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + identity.role().name())));
            } finally {
                attempts.release();
            }
        } catch (BadCredentialsException rejected) {
            audit.rejected();
            throw rejected;
        } catch (RuntimeException unavailable) {
            audit.unavailable();
            throw unavailable;
        }
    }

    @Override public boolean supports(Class<?> type) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(type);
    }
}

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
    public AccountAuthenticationProvider(AccountService accounts, LoginAttemptService attempts) {
        this.accounts = accounts; this.attempts = attempts;
    }
    @Override public Authentication authenticate(Authentication authentication) {
        String login = authentication.getName();
        attempts.acquire(login);
        try {
            var identity = accounts.verifyCredentials(login, (String) authentication.getCredentials())
                .orElseThrow(() -> new BadCredentialsException("Invalid login or password"));
            attempts.succeeded(login);
            return UsernamePasswordAuthenticationToken.authenticated(identity.login(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + identity.role().name())));
        } finally {
            attempts.release();
        }
    }
    @Override public boolean supports(Class<?> type) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(type);
    }
}

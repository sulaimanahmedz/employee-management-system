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
    public AccountAuthenticationProvider(AccountService accounts) { this.accounts = accounts; }
    @Override public Authentication authenticate(Authentication authentication) {
        var identity = accounts.verifyCredentials(authentication.getName(), (String) authentication.getCredentials())
            .orElseThrow(() -> new BadCredentialsException("Invalid login or password"));
        return UsernamePasswordAuthenticationToken.authenticated(identity.login(), null,
            List.of(new SimpleGrantedAuthority("ROLE_" + identity.role().name())));
    }
    @Override public boolean supports(Class<?> type) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(type);
    }
}

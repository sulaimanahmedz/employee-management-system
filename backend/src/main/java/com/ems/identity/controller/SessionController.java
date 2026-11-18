package com.ems.identity.controller;

import com.ems.identity.dto.CsrfResponse;
import com.ems.identity.dto.SessionIdentity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class SessionController {
    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) { return new CsrfResponse(token.getHeaderName(), token.getToken()); }
    @GetMapping("/session")
    public SessionIdentity session(Authentication authentication) {
        return new SessionIdentity(authentication.getName(), authentication.getAuthorities().stream()
            .map(authority -> authority.getAuthority()).toList());
    }
}

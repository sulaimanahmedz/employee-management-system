package com.ems.identity.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;

/** A session must not remain authenticated when recording its creation fails. */
@Service
public class SessionLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final AuthenticationAuditService audit;
    public SessionLoginSuccessHandler(AuthenticationAuditService audit) { this.audit = audit; }
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {
        try {
            audit.sessionEstablished();
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch (RuntimeException unavailable) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
    }
}

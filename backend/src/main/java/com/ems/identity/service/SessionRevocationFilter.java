package com.ems.identity.service;

import com.ems.identity.dto.SessionPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

/** Checks persisted security version on every authenticated request; never caches authorization. */
public class SessionRevocationFilter extends OncePerRequestFilter {
    private final SessionValidityService validity;
    public SessionRevocationFilter(SessionValidityService validity) { this.validity = validity; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
            if (!(authentication.getPrincipal() instanceof SessionPrincipal principal) || !validity.valid(principal)) {
                new SecurityContextLogoutHandler().logout(request, response, authentication);
                response.setStatus(401);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}

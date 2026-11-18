package com.ems.identity.service;

import org.springframework.security.authentication.ProviderManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.RequestCache;

@Configuration
public class IdentitySecurityConfiguration {
    @Bean
    SecurityFilterChain identitySecurity(HttpSecurity http, AccountAuthenticationProvider provider, AuthenticationAuditService audit, AccessAuditService accessAudit, RequestCache requestCache, SessionValidityService validity, SessionLoginSuccessHandler success) throws Exception {
        // A dedicated manager prevents retrying this same provider through an auto-configured parent.
        http.authenticationManager(new ProviderManager(provider))
            .requestCache(cache -> cache.requestCache(requestCache))
            .addFilterBefore(new SessionRevocationFilter(validity), org.springframework.security.web.csrf.CsrfFilter.class)
            .authorizeHttpRequests(access -> access
                .requestMatchers("/actuator/health", "/error", "/api/v1/auth/csrf", "/api/v1/auth/login", "/api/v1/auth/reset").permitAll()
                .requestMatchers("/api/v1/auth/session", "/api/v1/auth/logout", "/api/v1/auth/profile", "/api/v1/auth/password").authenticated()
                .requestMatchers("/api/v1/admin/accounts", "/api/v1/admin/accounts/**", "/api/v1/admin/audit").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/self/employees", "/api/v1/self/employees/*").hasAnyRole("MANAGER", "EMPLOYEE")
                .requestMatchers("/api/v1/employees", "/api/v1/employees/**").hasRole("HR")
                .anyRequest().denyAll())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) -> { accessAudit.authenticationRequired(); response.setStatus(401); })
                .accessDeniedHandler((request, response, exception) -> { accessAudit.denied(); response.setStatus(403); }))
            .formLogin(login -> login.loginProcessingUrl("/api/v1/auth/login")
                .successHandler(success)
                .failureHandler((request, response, exception) -> response.setStatus(401)))
            .logout(logout -> logout.logoutUrl("/api/v1/auth/logout")
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) -> { audit.logoutProcessed(); response.setStatus(204); }));
        // CSRF remains enabled with a server-session token. Default session fixation protection applies.
        return http.build();
    }
}

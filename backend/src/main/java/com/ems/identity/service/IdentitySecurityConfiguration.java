package com.ems.identity.service;

import org.springframework.security.authentication.ProviderManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class IdentitySecurityConfiguration {
    @Bean
    SecurityFilterChain identitySecurity(HttpSecurity http, AccountAuthenticationProvider provider) throws Exception {
        // A dedicated manager prevents retrying this same provider through an auto-configured parent.
        http.authenticationManager(new ProviderManager(provider))
            .authorizeHttpRequests(access -> access
                .requestMatchers("/actuator/health", "/error", "/api/v1/auth/csrf", "/api/v1/auth/login").permitAll()
                .requestMatchers("/api/v1/auth/session", "/api/v1/auth/logout").authenticated()
                .requestMatchers("/api/v1/admin/accounts").hasRole("ADMIN")
                .requestMatchers("/api/v1/employees", "/api/v1/employees/**").hasRole("HR")
                .anyRequest().denyAll())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) -> response.setStatus(401))
                .accessDeniedHandler((request, response, exception) -> response.setStatus(403)))
            .formLogin(login -> login.loginProcessingUrl("/api/v1/auth/login")
                .successHandler((request, response, authentication) -> response.setStatus(204))
                .failureHandler((request, response, exception) -> response.setStatus(401)))
            .logout(logout -> logout.logoutUrl("/api/v1/auth/logout")
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)));
        // CSRF remains enabled with a server-session token. Default session fixation protection applies.
        return http.build();
    }
}

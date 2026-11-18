package com.ems.identity.api;

import com.ems.identity.entity.AccountRole;
import com.ems.identity.service.AccountService;
import com.ems.identity.dto.SessionPrincipal;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class IdentityFixture {
    private IdentityFixture() {}
    public static RequestPostProcessor role(ApplicationContext context,String role) {
        var accounts=context.getBean(AccountService.class);
        String login="fixture-"+role.toLowerCase();
        var identity=accounts.findIdentity(login).orElseGet(() -> accounts.provision(login,UUID.randomUUID().toString(),AccountRole.valueOf(role)));
        return SecurityMockMvcRequestPostProcessors.authentication(UsernamePasswordAuthenticationToken.authenticated(
            new SessionPrincipal(identity.id(),identity.login(),identity.securityVersion()),null,
            List.of(new SimpleGrantedAuthority("ROLE_"+role))));
    }
}

package com.ems.identity.unit;
import com.ems.identity.dto.SessionPrincipal;
import com.ems.identity.entity.Account;
import com.ems.identity.entity.AccountRole;
import com.ems.identity.repository.AccountRepository;
import com.ems.identity.service.SessionValidityService;
import com.ems.identity.service.SessionRevocationFilter;
import java.util.Optional;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import jakarta.servlet.FilterChain;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class SessionRevocationTest {
    @Test void stampsRequireExistingEnabledAccountMatchingLoginAndVersion() {
        var repo=mock(AccountRepository.class);var service=new SessionValidityService(repo);
        var principal=new SessionPrincipal(1L,"synthetic",0);
        assertFalse(service.valid(principal));
        var a=new Account("synthetic","hash",AccountRole.HR);when(repo.findById(1L)).thenReturn(Optional.of(a));
        assertTrue(service.valid(principal));assertEquals("synthetic",principal.getName());
        assertFalse(service.valid(new SessionPrincipal(1L,"other",0)));
        assertFalse(service.valid(new SessionPrincipal(1L,"synthetic",1)));
        a.changeAccess(AccountRole.HR,false);assertFalse(service.valid(principal));
    }
    @Test void filterAllowsAnonymousAndValidSessionsButInvalidatesUnsupportedOrStalePrincipals() throws Exception {
        var validity=mock(SessionValidityService.class);var filter=new SessionRevocationFilter(validity);
        var chain=mock(FilterChain.class);
        try {
            filter.doFilter(new MockHttpServletRequest(),new MockHttpServletResponse(),chain);
            SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("key","anonymous",List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
            filter.doFilter(new MockHttpServletRequest(),new MockHttpServletResponse(),chain);
            var p=new SessionPrincipal(1L,"synthetic",0);when(validity.valid(p)).thenReturn(true);
            SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(p,null,List.of()));
            filter.doFilter(new MockHttpServletRequest(),new MockHttpServletResponse(),chain);
            verify(chain,times(3)).doFilter(any(),any());
            for(Object principal:new Object[]{"unsupported",new SessionPrincipal(2L,"removed",0)}) {
                SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal,null,List.of()));
                var request=new MockHttpServletRequest();var session=(org.springframework.mock.web.MockHttpSession)request.getSession();var response=new MockHttpServletResponse();
                filter.doFilter(request,response,chain);
                assertEquals(401,response.getStatus());assertTrue(session.isInvalid());assertNull(SecurityContextHolder.getContext().getAuthentication());
            }
            verifyNoMoreInteractions(chain);
        } finally {SecurityContextHolder.clearContext();}
    }
}

package com.ems.identity.unit;
import com.ems.identity.service.AuthenticationAuditService;
import com.ems.identity.service.SessionLoginSuccessHandler;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class SessionLoginSuccessTest {
 @Test void durableAuditFailureRevokesTheNewlySavedSession() {
  var audit=mock(AuthenticationAuditService.class);var handler=new SessionLoginSuccessHandler(audit);
  var request=new MockHttpServletRequest();var session=(MockHttpSession)request.getSession();var response=new MockHttpServletResponse();
  var authentication=new TestingAuthenticationToken("synthetic",null,"ROLE_HR");SecurityContextHolder.getContext().setAuthentication(authentication);
  try {
   doThrow(new IllegalStateException("synthetic storage failure")).when(audit).sessionEstablished();
   handler.onAuthenticationSuccess(request,response,authentication);
   assertEquals(503,response.getStatus());assertTrue(session.isInvalid());assertNull(SecurityContextHolder.getContext().getAuthentication());assertEquals("",response.getContentAsString());
  } catch(java.io.UnsupportedEncodingException impossible){throw new AssertionError(impossible);} finally{SecurityContextHolder.clearContext();}
 }
}

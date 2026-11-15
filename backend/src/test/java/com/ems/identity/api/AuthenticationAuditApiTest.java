package com.ems.identity.api;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ems.identity.entity.AccountRole;
import com.ems.identity.repository.AccountRepository;
import com.ems.identity.service.AccountService;
import com.ems.identity.service.AuthenticationAuditService;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:authentication-audit;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class AuthenticationAuditApiTest {
    @Autowired MockMvc mvc;
    @Autowired AccountService accounts;
    @Autowired AccountRepository repository;

    @Test void realCredentialAndThrottleFailuresEmitSafeRecordsAndKeepGenericHttpErrors() throws Exception {
        repository.deleteAll();
        var identity = accounts.provision("synthetic-audit", "Synthetic audit password!", AccountRole.HR);
        Logger logger = (Logger) LoggerFactory.getLogger(AuthenticationAuditService.class);
        var appender = new ListAppender<ILoggingEvent>(); appender.start(); logger.addAppender(appender);
        try {
            mvc.perform(post("/api/v1/auth/login").with(csrf()).param("username", " SYNTHETIC-AUDIT ")
                .param("password", "Synthetic audit password!")).andExpect(status().isNoContent());
            for (int i = 0; i < 6; i++) {
                mvc.perform(post("/api/v1/auth/login").with(csrf()).param("username", "synthetic-audit")
                    .param("password", "incorrect-secret")).andExpect(status().isUnauthorized()).andExpect(content().string(""));
            }
            mvc.perform(post("/api/v1/auth/login").with(csrf()).param("username", "unknown\nforged-event")
                .param("password", "another-secret")).andExpect(status().isUnauthorized()).andExpect(content().string(""));
            assertEquals(9, appender.list.size());
            assertEquals("event=AUTH_CREDENTIALS_ACCEPTED account_id=" + identity.id(), appender.list.get(0).getFormattedMessage());
            assertEquals("event=AUTH_SESSION_ESTABLISHED", appender.list.get(1).getFormattedMessage());
            for (var event : appender.list.subList(2, 9)) {
                assertEquals("event=AUTH_CREDENTIALS_REJECTED", event.getFormattedMessage());
                assertNull(event.getArgumentArray()); assertNull(event.getThrowableProxy());
            }
            mvc.perform(post("/api/v1/auth/login").param("username", "synthetic-audit").param("password", "secret"))
                .andExpect(status().isForbidden());
            assertEquals(9, appender.list.size(), "CSRF rejection occurs before credential verification and is outside this audit scope");
            var recovery = accounts.provision("synthetic-audit-recovery", "Synthetic recovery password!", AccountRole.HR);
            for (int i = 0; i < 4; i++) {
                mvc.perform(post("/api/v1/auth/login").with(csrf()).param("username", "synthetic-audit-recovery")
                    .param("password", "wrong")).andExpect(status().isUnauthorized());
            }
            mvc.perform(post("/api/v1/auth/login").with(csrf()).param("username", "synthetic-audit-recovery")
                .param("password", "Synthetic recovery password!")).andExpect(status().isNoContent());
            assertEquals(15, appender.list.size(), "Each credential attempt runs once; each successful login also records session establishment");
            assertEquals("event=AUTH_CREDENTIALS_ACCEPTED account_id=" + recovery.id(), appender.list.get(13).getFormattedMessage());
            assertEquals("event=AUTH_SESSION_ESTABLISHED", appender.list.get(14).getFormattedMessage());
        } finally { logger.detachAppender(appender); appender.stop(); repository.deleteAll(); }
    }
    @Test void logoutRecordsOnlyCompletedProcessingAndInvalidatesAuthenticatedSession() throws Exception {
        accounts.provision("synthetic-logout-audit", "Synthetic logout password!", AccountRole.EMPLOYEE);
        var login = mvc.perform(post("/api/v1/auth/login").with(csrf()).param("username", "synthetic-logout-audit")
            .param("password", "Synthetic logout password!")).andExpect(status().isNoContent()).andReturn();
        var session = (org.springframework.mock.web.MockHttpSession) login.getRequest().getSession(false);
        Logger logger = (Logger) LoggerFactory.getLogger(AuthenticationAuditService.class);
        var appender = new ListAppender<ILoggingEvent>(); appender.start(); logger.addAppender(appender);
        try {
            mvc.perform(post("/api/v1/auth/logout").session(session)).andExpect(status().isForbidden());
            assertFalse(session.isInvalid()); assertTrue(appender.list.isEmpty());
            mvc.perform(post("/api/v1/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
            assertTrue(session.isInvalid()); assertEquals(1, appender.list.size());
            // Logout is idempotent: a valid anonymous CSRF request still completes processing.
            mvc.perform(post("/api/v1/auth/logout").with(csrf())).andExpect(status().isNoContent());
            assertEquals(2, appender.list.size());
            for (var event : appender.list) {
                assertEquals("event=AUTH_LOGOUT_PROCESSED", event.getFormattedMessage());
                assertNull(event.getArgumentArray()); assertNull(event.getThrowableProxy());
            }
        } finally { logger.detachAppender(appender); appender.stop(); repository.deleteAll(); }
    }
}

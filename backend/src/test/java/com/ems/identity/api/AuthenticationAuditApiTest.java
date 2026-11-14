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
            assertEquals(8, appender.list.size());
            assertEquals("event=AUTH_CREDENTIALS_ACCEPTED account_id=" + identity.id(), appender.list.get(0).getFormattedMessage());
            for (var event : appender.list.subList(1, 8)) {
                assertEquals("event=AUTH_CREDENTIALS_REJECTED", event.getFormattedMessage());
                assertNull(event.getArgumentArray()); assertNull(event.getThrowableProxy());
            }
            mvc.perform(post("/api/v1/auth/login").param("username", "synthetic-audit").param("password", "secret"))
                .andExpect(status().isForbidden());
            assertEquals(8, appender.list.size(), "CSRF rejection occurs before credential verification and is outside this audit scope");
            var recovery = accounts.provision("synthetic-audit-recovery", "Synthetic recovery password!", AccountRole.HR);
            for (int i = 0; i < 4; i++) {
                mvc.perform(post("/api/v1/auth/login").with(csrf()).param("username", "synthetic-audit-recovery")
                    .param("password", "wrong")).andExpect(status().isUnauthorized());
            }
            mvc.perform(post("/api/v1/auth/login").with(csrf()).param("username", "synthetic-audit-recovery")
                .param("password", "Synthetic recovery password!")).andExpect(status().isNoContent());
            assertEquals(13, appender.list.size(), "Each HTTP credential attempt must invoke the provider exactly once");
            assertEquals("event=AUTH_CREDENTIALS_ACCEPTED account_id=" + recovery.id(), appender.list.get(12).getFormattedMessage());
        } finally { logger.detachAppender(appender); appender.stop(); repository.deleteAll(); }
    }
}

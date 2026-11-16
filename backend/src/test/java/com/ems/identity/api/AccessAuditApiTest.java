package com.ems.identity.api;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ems.identity.service.AccessAuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:access-audit;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class AccessAuditApiTest {
    @Autowired MockMvc mvc;

    @Test void anonymousAndCsrfRejectionsKeepEmptyErrorsAndNoRequestData() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(AccessAuditService.class);
        var appender = new ListAppender<ILoggingEvent>(); appender.start(); logger.addAppender(appender);
        try {
            mvc.perform(get("/api/v1/employees").queryParam("secret", "sensitive-query")
                .header("X-Forwarded-For", "forged-source")).andExpect(status().isUnauthorized()).andExpect(content().string(""));
            mvc.perform(post("/api/v1/auth/login").param("username", "injected\nrecord").param("password", "secret"))
                .andExpect(status().isForbidden()).andExpect(content().string(""));
            assertEquals(2, appender.list.size());
            assertEquals("event=ACCESS_AUTHENTICATION_REQUIRED", appender.list.get(0).getFormattedMessage());
            assertEquals("event=ACCESS_DENIED", appender.list.get(1).getFormattedMessage());
            for (var event : appender.list) { assertNull(event.getArgumentArray()); assertNull(event.getThrowableProxy()); }
        } finally { logger.detachAppender(appender); appender.stop(); }
    }

    @ParameterizedTest @ValueSource(strings = {"ADMIN", "HR", "MANAGER", "EMPLOYEE"})
    void deniedRouteIsAuditedAndAllowedRoleRequestsStaySilent(String role) throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(AccessAuditService.class);
        var appender = new ListAppender<ILoggingEvent>(); appender.start(); logger.addAppender(appender);
        try {
            mvc.perform(get("/api/v1/unassigned").with(user("untrusted\nprincipal").roles(role)))
                .andExpect(status().isForbidden()).andExpect(content().string(""));
            mvc.perform(get("/api/v1/auth/session").with(user("synthetic-user").roles(role)))
                .andExpect(status().isOk()).andExpect(jsonPath("login").value("synthetic-user"));
            assertEquals(1, appender.list.size());
            assertEquals("event=ACCESS_DENIED", appender.list.get(0).getFormattedMessage());
            assertNull(appender.list.get(0).getArgumentArray()); assertNull(appender.list.get(0).getThrowableProxy());
        } finally { logger.detachAppender(appender); appender.stop(); }
    }
}

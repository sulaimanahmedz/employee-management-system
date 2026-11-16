package com.ems.identity.unit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ems.identity.service.AccessAuditService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import static org.junit.jupiter.api.Assertions.*;

class AccessAuditTest {
    @Test void recordsFixedCodesWithoutArgumentsOrThrowable() {
        Logger logger = (Logger) LoggerFactory.getLogger(AccessAuditService.class);
        var appender = new ListAppender<ILoggingEvent>(); appender.start(); logger.addAppender(appender);
        try {
            var audit = new AccessAuditService();
            audit.authenticationRequired(); audit.denied();
            assertEquals(List.of("event=ACCESS_AUTHENTICATION_REQUIRED", "event=ACCESS_DENIED"),
                appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList());
            for (var event : appender.list) {
                assertEquals(Level.INFO, event.getLevel());
                assertNull(event.getArgumentArray()); assertNull(event.getThrowableProxy());
                assertTrue(event.getTimeStamp() > 0);
            }
        } finally { logger.detachAppender(appender); appender.stop(); }
    }
}

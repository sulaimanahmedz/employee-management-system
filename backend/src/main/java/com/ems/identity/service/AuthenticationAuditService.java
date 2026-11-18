package com.ems.identity.service;

import com.ems.identity.entity.SecurityEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Minimal authentication records; never accept request strings or exceptions. */
@Service
public class AuthenticationAuditService {
    private static final Logger AUDIT = LoggerFactory.getLogger(AuthenticationAuditService.class);
    private final AuditTrailService trail;
    public AuthenticationAuditService(AuditTrailService trail) { this.trail=trail; }

    public void accepted(Long accountId) {
        trail.record(SecurityEvent.Action.CREDENTIALS_ACCEPTED, accountId, accountId);
        AUDIT.info("event=AUTH_CREDENTIALS_ACCEPTED account_id={}", accountId);
    }

    public void rejected() {
        trail.record(SecurityEvent.Action.CREDENTIALS_REJECTED, null, null);
        AUDIT.info("event=AUTH_CREDENTIALS_REJECTED");
    }

    public void unavailable() {
        trail.record(SecurityEvent.Action.VERIFICATION_UNAVAILABLE, null, null);
        AUDIT.error("event=AUTH_VERIFICATION_UNAVAILABLE");
    }
    public void sessionEstablished() {
        trail.record(SecurityEvent.Action.SESSION_ESTABLISHED, null, null);
        AUDIT.info("event=AUTH_SESSION_ESTABLISHED");
    }

    public void logoutProcessed() {
        trail.record(SecurityEvent.Action.LOGOUT_PROCESSED, null, null);
        AUDIT.info("event=AUTH_LOGOUT_PROCESSED");
    }
}

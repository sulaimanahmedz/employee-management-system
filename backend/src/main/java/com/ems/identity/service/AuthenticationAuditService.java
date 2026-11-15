package com.ems.identity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Minimal authentication records; never accept request strings or exceptions. */
@Service
public class AuthenticationAuditService {
    private static final Logger AUDIT = LoggerFactory.getLogger(AuthenticationAuditService.class);

    public void accepted(Long accountId) {
        AUDIT.info("event=AUTH_CREDENTIALS_ACCEPTED account_id={}", accountId);
    }

    public void rejected() {
        AUDIT.info("event=AUTH_CREDENTIALS_REJECTED");
    }

    public void unavailable() {
        AUDIT.error("event=AUTH_VERIFICATION_UNAVAILABLE");
    }
    public void sessionEstablished() {
        AUDIT.info("event=AUTH_SESSION_ESTABLISHED");
    }

    public void logoutProcessed() {
        AUDIT.info("event=AUTH_LOGOUT_PROCESSED");
    }
}

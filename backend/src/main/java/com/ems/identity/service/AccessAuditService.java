package com.ems.identity.service;

import com.ems.identity.entity.SecurityEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Fixed security-filter rejection events; accepts no request data or exceptions. */
@Service
public class AccessAuditService {
    private static final Logger AUDIT = LoggerFactory.getLogger(AccessAuditService.class);
    private final AuditTrailService trail;
    public AccessAuditService(AuditTrailService trail) { this.trail=trail; }

    public void authenticationRequired() {
        trail.record(SecurityEvent.Action.AUTHENTICATION_REQUIRED, null, null);
        AUDIT.info("event=ACCESS_AUTHENTICATION_REQUIRED");
    }

    public void denied() {
        trail.record(SecurityEvent.Action.ACCESS_DENIED, null, null);
        AUDIT.info("event=ACCESS_DENIED");
    }
}

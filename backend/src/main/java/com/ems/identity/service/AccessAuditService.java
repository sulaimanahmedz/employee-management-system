package com.ems.identity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Fixed security-filter rejection events; accepts no request data or exceptions. */
@Service
public class AccessAuditService {
    private static final Logger AUDIT = LoggerFactory.getLogger(AccessAuditService.class);

    public void authenticationRequired() {
        AUDIT.info("event=ACCESS_AUTHENTICATION_REQUIRED");
    }

    public void denied() {
        AUDIT.info("event=ACCESS_DENIED");
    }
}

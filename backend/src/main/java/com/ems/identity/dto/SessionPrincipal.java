package com.ems.identity.dto;

import java.io.Serializable;
import java.security.Principal;

public record SessionPrincipal(Long accountId, String login, long securityVersion) implements Principal, Serializable {
    @Override public String getName() { return login; }
}

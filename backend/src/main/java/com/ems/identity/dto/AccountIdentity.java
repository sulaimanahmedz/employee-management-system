package com.ems.identity.dto;

import com.ems.identity.entity.AccountRole;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** Credential-free response. Internal version is excluded from JSON. */
public record AccountIdentity(Long id, String login, AccountRole role, @JsonIgnore long securityVersion) {
    public AccountIdentity(Long id, String login, AccountRole role) { this(id, login, role, 0); }
}

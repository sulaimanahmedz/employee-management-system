package com.ems.identity.dto;

import com.ems.identity.entity.AccountRole;

/** Credential-free identity; this does not confer authorization or create a session. */
public record AccountIdentity(Long id, String login, AccountRole role) {}

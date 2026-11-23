package com.ems.identity.dto;

import com.ems.identity.entity.AccountRole;

/** Administrative directory projection; excludes credentials and internal security state. */
public record AccountDirectoryEntry(Long id, String login, AccountRole role, boolean enabled) {}

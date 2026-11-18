package com.ems.identity.dto;
import com.ems.identity.entity.AccountRole;
public record AccountAccessView(Long id, String login, AccountRole role, boolean enabled) {}

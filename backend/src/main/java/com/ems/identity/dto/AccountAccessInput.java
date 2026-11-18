package com.ems.identity.dto;
import com.ems.identity.entity.AccountRole;
import jakarta.validation.constraints.NotNull;
public record AccountAccessInput(@NotNull AccountRole role, @NotNull Boolean enabled) {}

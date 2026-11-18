package com.ems.identity.dto;

import com.ems.identity.entity.AccountRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountInput(@NotBlank @Size(max = 64) String login,
    @NotBlank @Size(max = 72) String password, @NotNull AccountRole role) {}

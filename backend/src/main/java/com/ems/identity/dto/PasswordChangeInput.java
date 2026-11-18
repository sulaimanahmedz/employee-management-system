package com.ems.identity.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record PasswordChangeInput(@NotBlank @Size(max=72) String currentPassword, @NotBlank @Size(max=72) String newPassword) {}

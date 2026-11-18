package com.ems.identity.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record ResetInput(@NotBlank @Size(max=64) String token, @NotBlank @Size(max=72) String newPassword) {}

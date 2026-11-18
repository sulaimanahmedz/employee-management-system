package com.ems.identity.dto;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
public record ScopeInput(@Positive Long employeeId, @NotNull @Size(max=100) List<@NotNull @Positive Long> teamEmployeeIds) {}

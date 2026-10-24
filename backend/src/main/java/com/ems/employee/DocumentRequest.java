package com.ems.employee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record DocumentRequest(
    @NotBlank @Size(max = 150) String name,
    @NotNull EmployeeDocument.Category category,
    LocalDate expiresOn
) {}

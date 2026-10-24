package com.ems.employee.dto;

import com.ems.employee.entity.EmployeeStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmployeeRequest(
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(max = 100) String department,
    @NotBlank @Size(max = 100) String jobTitle,
    @NotNull EmployeeStatus status
) {}

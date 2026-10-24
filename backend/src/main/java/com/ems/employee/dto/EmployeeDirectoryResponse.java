package com.ems.employee.dto;

import java.util.List;

public record EmployeeDirectoryResponse(
    List<EmployeeResponse> items, int page, int size, long totalElements, int totalPages,
    long totalEmployees, long activeEmployees, long departments
) {}

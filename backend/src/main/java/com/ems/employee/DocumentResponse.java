package com.ems.employee;

import java.time.LocalDate;

public record DocumentResponse(Long id, String name, EmployeeDocument.Category category, LocalDate expiresOn) {
    public static DocumentResponse from(EmployeeDocument document) {
        return new DocumentResponse(document.getId(), document.getName(), document.getCategory(), document.getExpiresOn());
    }
}

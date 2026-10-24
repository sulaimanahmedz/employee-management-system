package com.ems.document.dto;

import com.ems.document.entity.EmployeeDocument;

import java.time.LocalDate;

public record DocumentResponse(Long id, String name, EmployeeDocument.Category category, LocalDate expiresOn) {
    public record Page(java.util.List<DocumentResponse> items, int page, int totalPages, long totalElements) {}
    public static DocumentResponse from(EmployeeDocument document) {
        return new DocumentResponse(document.getId(), document.getName(), document.getCategory(), document.getExpiresOn());
    }
}

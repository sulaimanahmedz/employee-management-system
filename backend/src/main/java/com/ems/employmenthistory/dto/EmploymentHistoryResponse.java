package com.ems.employmenthistory.dto;

import com.ems.employee.entity.EmployeeStatus;
import com.ems.employmenthistory.entity.EmploymentHistory;

import java.time.Instant;
import java.util.List;

public record EmploymentHistoryResponse(List<Entry> items, int page, int size, long totalElements, int totalPages) {
    public record Entry(Long id, Instant recordedAt, EmploymentHistory.Kind kind,
                        String department, String jobTitle, EmployeeStatus status) {
        public static Entry from(EmploymentHistory entry) {
            return new Entry(entry.getId(), entry.getRecordedAt(), entry.getKind(),
                entry.getDepartment(), entry.getJobTitle(), entry.getStatus());
        }
    }
}

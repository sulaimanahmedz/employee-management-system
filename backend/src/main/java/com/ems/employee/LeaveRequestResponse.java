package com.ems.employee;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public record LeaveRequestResponse(Long id, LeaveRequest.Type type, LocalDate startOn, LocalDate endOn,
        LeaveRequest.Status status, long calendarDays) {
    public static LeaveRequestResponse from(LeaveRequest request) {
        return new LeaveRequestResponse(request.getId(), request.getType(), request.getStartOn(), request.getEndOn(),
            request.getStatus(), ChronoUnit.DAYS.between(request.getStartOn(), request.getEndOn()) + 1);
    }
    public record Page(List<LeaveRequestResponse> items, int page, int totalPages, long totalElements) {}
}

package com.ems.employee;

import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/employees/{employeeId}/leave-requests")
public class LeaveRequestController {
    private final LeaveRequestService service;
    public LeaveRequestController(LeaveRequestService service) { this.service = service; }
    @GetMapping
    public LeaveRequestResponse.Page list(@PathVariable Long employeeId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) LeaveRequest.Status status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.list(employeeId, page, status, from, to);
    }
    @GetMapping("/{requestId}")
    public LeaveRequestResponse get(@PathVariable Long employeeId, @PathVariable Long requestId) {
        return service.get(employeeId, requestId);
    }
    @PostMapping
    public ResponseEntity<LeaveRequestResponse> create(@PathVariable Long employeeId, @Valid @RequestBody LeaveRequestInput input) {
        var request = service.create(employeeId, input);
        return ResponseEntity.created(URI.create("/api/v1/employees/" + employeeId + "/leave-requests/" + request.id())).body(request);
    }
    @PutMapping("/{requestId}/cancellation")
    public LeaveRequestResponse cancel(@PathVariable Long employeeId, @PathVariable Long requestId) {
        return service.cancel(employeeId, requestId);
    }
}

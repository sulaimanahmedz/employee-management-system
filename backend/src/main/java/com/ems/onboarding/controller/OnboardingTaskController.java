package com.ems.onboarding.controller;

import com.ems.onboarding.dto.OnboardingTaskCompletionRequest;
import com.ems.onboarding.dto.OnboardingTaskRequest;
import com.ems.onboarding.dto.OnboardingTaskResponse;
import com.ems.onboarding.service.OnboardingTaskService;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/employees/{employeeId}/onboarding-tasks")
public class OnboardingTaskController {
    private final OnboardingTaskService service;
    public OnboardingTaskController(OnboardingTaskService service) { this.service = service; }
    @GetMapping
    public OnboardingTaskResponse.Page list(@PathVariable Long employeeId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dueBy) {
        return service.list(employeeId, page, completed, dueBy);
    }
    @GetMapping("/{taskId}")
    public OnboardingTaskResponse get(@PathVariable Long employeeId, @PathVariable Long taskId) {
        return service.get(employeeId, taskId);
    }
    @PostMapping
    public ResponseEntity<OnboardingTaskResponse> create(@PathVariable Long employeeId, @Valid @RequestBody OnboardingTaskRequest request) {
        var task = service.create(employeeId, request);
        return ResponseEntity.created(URI.create("/api/v1/employees/" + employeeId + "/onboarding-tasks/" + task.id())).body(task);
    }
    @PutMapping("/{taskId}")
    public OnboardingTaskResponse update(@PathVariable Long employeeId, @PathVariable Long taskId,
            @Valid @RequestBody OnboardingTaskRequest request) { return service.update(employeeId, taskId, request); }
    @PutMapping("/{taskId}/completion")
    public OnboardingTaskResponse complete(@PathVariable Long employeeId, @PathVariable Long taskId,
            @Valid @RequestBody OnboardingTaskCompletionRequest request) { return service.complete(employeeId, taskId, request.completed()); }
    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long employeeId, @PathVariable Long taskId) { service.delete(employeeId, taskId); }
}

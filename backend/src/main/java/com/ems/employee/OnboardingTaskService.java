package com.ems.employee;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class OnboardingTaskService {
    private final EmployeeRepository employees;
    private final OnboardingTaskRepository tasks;
    public OnboardingTaskService(EmployeeRepository employees, OnboardingTaskRepository tasks) {
        this.employees = employees; this.tasks = tasks;
    }
    private Employee employee(Long id) {
        return employees.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }
    private OnboardingTask task(Long employeeId, Long taskId) {
        return tasks.findByIdAndEmployeeId(taskId, employeeId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Onboarding task not found"));
    }
    public OnboardingTaskResponse.Page list(Long employeeId, int page) {
        employee(employeeId);
        if (page < 0 || (long) page * 10 > Integer.MAX_VALUE)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task page");
        var result = tasks.findByEmployeeId(employeeId, PageRequest.of(page, 10, Sort.by("id").descending()));
        return new OnboardingTaskResponse.Page(result.getContent().stream().map(OnboardingTaskResponse::from).toList(),
            page, result.getTotalPages(), result.getTotalElements(), tasks.countByEmployeeIdAndCompletedTrue(employeeId));
    }
    public OnboardingTaskResponse get(Long employeeId, Long taskId) {
        employee(employeeId); return OnboardingTaskResponse.from(task(employeeId, taskId));
    }
    @Transactional
    public OnboardingTaskResponse create(Long employeeId, OnboardingTaskRequest request) {
        return OnboardingTaskResponse.from(tasks.saveAndFlush(new OnboardingTask(employee(employeeId), request)));
    }
    private OnboardingTask lockedTask(Long employeeId, Long taskId) {
        return tasks.findForUpdate(taskId, employeeId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Onboarding task not found"));
    }
    @Transactional
    public OnboardingTaskResponse update(Long employeeId, Long taskId, OnboardingTaskRequest request) {
        employee(employeeId);
        var task = lockedTask(employeeId, taskId);
        task.updateDetails(request);
        return OnboardingTaskResponse.from(tasks.saveAndFlush(task));
    }
    @Transactional
    public OnboardingTaskResponse complete(Long employeeId, Long taskId, boolean completed) {
        employee(employeeId);
        var task = lockedTask(employeeId, taskId); task.setCompleted(completed);
        return OnboardingTaskResponse.from(tasks.saveAndFlush(task));
    }
    @Transactional
    public void delete(Long employeeId, Long taskId) { employee(employeeId); tasks.delete(task(employeeId, taskId)); }
}

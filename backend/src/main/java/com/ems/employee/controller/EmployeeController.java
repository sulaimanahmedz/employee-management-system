package com.ems.employee.controller;

import com.ems.employee.dto.EmployeeDirectoryResponse;
import com.ems.employee.dto.EmployeeRequest;
import com.ems.employee.dto.EmployeeResponse;
import com.ems.employee.entity.EmployeeStatus;
import com.ems.employee.service.EmployeeService;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {
    private final EmployeeService service;
    public EmployeeController(EmployeeService service) { this.service = service; }
    @GetMapping
    public List<EmployeeResponse> list() { return service.list(); }
    @GetMapping("/directory")
    public EmployeeDirectoryResponse directory(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return service.directory(q, status, page, size);
    }
    @GetMapping("/{id}")
    public EmployeeResponse get(@PathVariable Long id) { return service.get(id); }
    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse employee = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/employees/" + employee.id())).body(employee);
    }
    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return service.update(id, request);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

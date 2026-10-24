package com.ems.employmenthistory.controller;

import com.ems.employmenthistory.dto.EmploymentHistoryResponse;
import com.ems.employmenthistory.service.EmploymentHistoryService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employees/{id}/history")
public class EmploymentHistoryController {
    private final EmploymentHistoryService service;

    public EmploymentHistoryController(EmploymentHistoryService service) {
        this.service = service;
    }

    @GetMapping
    public EmploymentHistoryResponse history(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return service.history(id, page, size);
    }
}

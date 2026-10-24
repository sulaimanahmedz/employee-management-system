package com.ems.employmenthistory.service;

import com.ems.employee.entity.Employee;
import com.ems.employee.repository.EmployeeRepository;
import com.ems.employmenthistory.dto.EmploymentHistoryResponse;
import com.ems.employmenthistory.entity.EmploymentHistory;
import com.ems.employmenthistory.repository.EmploymentHistoryRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class EmploymentHistoryService {
    private final EmployeeRepository employees;
    private final EmploymentHistoryRepository history;

    public EmploymentHistoryService(EmployeeRepository employees, EmploymentHistoryRepository history) {
        this.employees = employees;
        this.history = history;
    }

    public EmploymentHistoryResponse history(Long id, int page, int size) {
        if (!employees.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid history page or size");
        }
        var result = history.findByEmployeeId(id, PageRequest.of(page, size, Sort.by("id").descending()));
        return new EmploymentHistoryResponse(result.getContent().stream().map(EmploymentHistoryResponse.Entry::from).toList(),
            result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordCreated(Employee employee) {
        history.save(new EmploymentHistory(employee, EmploymentHistory.Kind.CREATED));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordBaselineIfMissing(Employee employee) {
        if (!history.existsByEmployeeId(employee.getId())) {
            history.save(new EmploymentHistory(employee, EmploymentHistory.Kind.BASELINE));
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordChanged(Employee employee) {
        history.save(new EmploymentHistory(employee, EmploymentHistory.Kind.CHANGED));
    }
}

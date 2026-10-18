package com.ems.employee;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class EmployeeService {
    private final EmployeeRepository repository;
    public EmployeeService(EmployeeRepository repository) { this.repository = repository; }
    public List<EmployeeResponse> list() {
        return repository.findAll(Sort.by("id")).stream().map(EmployeeResponse::from).toList();
    }
    public EmployeeResponse get(Long id) { return EmployeeResponse.from(find(id)); }
    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        return EmployeeResponse.from(repository.saveAndFlush(new Employee(request)));
    }
    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = find(id);
        employee.update(request);
        return EmployeeResponse.from(repository.saveAndFlush(employee));
    }
    @Transactional
    public void delete(Long id) { repository.delete(find(id)); }
    private Employee find(Long id) {
        return repository.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }
}

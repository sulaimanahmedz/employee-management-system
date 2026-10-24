package com.ems.employee.service;

import com.ems.employee.dto.EmployeeDirectoryResponse;
import com.ems.employee.dto.EmployeeRequest;
import com.ems.employee.dto.EmployeeResponse;
import com.ems.employee.entity.Employee;
import com.ems.employee.entity.EmployeeStatus;
import com.ems.employee.repository.EmployeeRepository;
import com.ems.employmenthistory.service.EmploymentHistoryService;

import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class EmployeeService {
    private final EmployeeRepository repository;
    private final EmploymentHistoryService history;
    private final jakarta.persistence.EntityManager entityManager;
    public EmployeeService(EmployeeRepository repository, EmploymentHistoryService history,
                           jakarta.persistence.EntityManager entityManager) {
        this.repository = repository;
        this.history = history;
        this.entityManager = entityManager;
    }
    public List<EmployeeResponse> list() {
        return repository.findAll(Sort.by("id")).stream().map(EmployeeResponse::from).toList();
    }
    public EmployeeDirectoryResponse directory(String query, EmployeeStatus status, int page, int size, String sortBy, String direction) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Page must be non-negative, size must be 1 to 100, and the page offset must fit a signed integer");
        }
        if (query.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search must be at most 200 characters");
        }
        Sort order = directoryOrder(sortBy, direction);
        String term = query.strip().toLowerCase(Locale.ROOT);
        Specification<Employee> matching = (root, criteria, builder) -> {
            var textMatch = term.isEmpty() ? builder.conjunction() : builder.or(
                builder.greaterThan(builder.locate(builder.lower(
                    builder.concat(builder.concat(root.<String>get("firstName"), " "),
                        root.<String>get("lastName"))), term), 0),
                builder.greaterThan(builder.locate(builder.lower(root.<String>get("email")), term), 0),
                builder.greaterThan(builder.locate(builder.lower(root.<String>get("department")), term), 0),
                builder.greaterThan(builder.locate(builder.lower(root.<String>get("jobTitle")), term), 0)
            );
            return status == null ? textMatch :
                builder.and(textMatch, builder.equal(root.get("status"), status));
        };
        var result = repository.findAll(matching, PageRequest.of(page, size, order));
        return new EmployeeDirectoryResponse(
            result.getContent().stream().map(EmployeeResponse::from).toList(),
            result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(),
            repository.count(), repository.countByStatus(EmployeeStatus.ACTIVE), repository.countDepartments());
    }

    private Sort directoryOrder(String sortBy, String direction) {
        Sort.Direction order = switch (direction) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Direction must be asc or desc");
        };
        // Only fixed property names reach Spring Data; never pass through arbitrary client paths.
        Sort selected = switch (sortBy) {
            case "id" -> Sort.by(order, "id");
            case "name" -> Sort.by(new Sort.Order(order, "lastName").ignoreCase(),
                new Sort.Order(order, "firstName").ignoreCase());
            case "department" -> Sort.by(new Sort.Order(order, "department").ignoreCase());
            case "jobTitle" -> Sort.by(new Sort.Order(order, "jobTitle").ignoreCase());
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported directory sort field");
        };
        // A fixed unique tie-breaker keeps equal values stable across page boundaries.
        return sortBy.equals("id") ? selected : selected.and(Sort.by("id"));
    }

    public EmployeeResponse get(Long id) { return EmployeeResponse.from(find(id)); }
    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        Employee employee = repository.saveAndFlush(new Employee(request.firstName(), request.lastName(), request.email(), request.department(), request.jobTitle(), request.status()));
        history.recordCreated(employee);
        return EmployeeResponse.from(employee);
    }
    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        // Serialize updates so each snapshot describes the preceding committed state.
        Employee employee = entityManager.find(Employee.class, id, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (employee == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
        history.recordBaselineIfMissing(employee);
        boolean changed = !employee.getDepartment().equals(request.department().strip())
            || !employee.getJobTitle().equals(request.jobTitle().strip()) || employee.getStatus() != request.status();
        employee.update(request.firstName(), request.lastName(), request.email(), request.department(), request.jobTitle(), request.status());
        repository.saveAndFlush(employee);
        if (changed) history.recordChanged(employee);
        return EmployeeResponse.from(employee);
    }
    @Transactional
    public void delete(Long id) { repository.delete(find(id)); }
    private Employee find(Long id) {
        return repository.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }
}

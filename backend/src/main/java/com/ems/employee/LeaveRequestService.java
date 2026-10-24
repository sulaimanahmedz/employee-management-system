package com.ems.employee;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class LeaveRequestService {
    private final EmployeeRepository employees;
    private final LeaveRequestRepository requests;
    private final EntityManager entityManager;
    public LeaveRequestService(EmployeeRepository employees, LeaveRequestRepository requests, EntityManager entityManager) {
        this.employees = employees; this.requests = requests; this.entityManager = entityManager;
    }
    private void requireEmployee(Long id) {
        if (!employees.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
    }
    // Serialize creation/cancellation for one employee, including the overlap check.
    private Employee lockEmployee(Long id) {
        var employee = entityManager.find(Employee.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (employee == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
        return employee;
    }
    private LeaveRequest request(Long employeeId, Long requestId) {
        return requests.findByIdAndEmployeeId(requestId, employeeId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found"));
    }
    public LeaveRequestResponse.Page list(Long employeeId, int page) {
        requireEmployee(employeeId);
        if (page < 0 || (long) page * 10 > Integer.MAX_VALUE)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid leave request page");
        var result = requests.findByEmployeeId(employeeId, PageRequest.of(page, 10, Sort.by("id").descending()));
        return new LeaveRequestResponse.Page(result.getContent().stream().map(LeaveRequestResponse::from).toList(),
            page, result.getTotalPages(), result.getTotalElements());
    }
    public LeaveRequestResponse get(Long employeeId, Long requestId) {
        requireEmployee(employeeId); return LeaveRequestResponse.from(request(employeeId, requestId));
    }
    @Transactional
    public LeaveRequestResponse create(Long employeeId, LeaveRequestInput input) {
        var employee = lockEmployee(employeeId);
        if (input.endOn().isBefore(input.startOn()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must be on or after start date");
        if (requests.existsByEmployeeIdAndStatusAndStartOnLessThanEqualAndEndOnGreaterThanEqual(
                employeeId, LeaveRequest.Status.PENDING, input.endOn(), input.startOn()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Dates overlap an existing pending leave request");
        return LeaveRequestResponse.from(requests.saveAndFlush(new LeaveRequest(employee, input)));
    }
    @Transactional
    public LeaveRequestResponse cancel(Long employeeId, Long requestId) {
        lockEmployee(employeeId);
        var request = request(employeeId, requestId); request.cancel();
        return LeaveRequestResponse.from(requests.saveAndFlush(request));
    }
}

package com.ems.employee;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    Page<LeaveRequest> findByEmployeeId(Long employeeId, Pageable pageable);
    Optional<LeaveRequest> findByIdAndEmployeeId(Long id, Long employeeId);
    boolean existsByEmployeeIdAndStatusAndStartOnLessThanEqualAndEndOnGreaterThanEqual(
        Long employeeId, LeaveRequest.Status status, LocalDate endOn, LocalDate startOn);
}

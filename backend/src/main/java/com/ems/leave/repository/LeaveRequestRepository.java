package com.ems.leave.repository;

import com.ems.leave.entity.LeaveRequest;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long>, JpaSpecificationExecutor<LeaveRequest> {
    Optional<LeaveRequest> findByIdAndEmployeeId(Long id, Long employeeId);
    boolean existsByEmployeeIdAndStatusAndStartOnLessThanEqualAndEndOnGreaterThanEqual(
        Long employeeId, LeaveRequest.Status status, LocalDate endOn, LocalDate startOn);
    boolean existsByEmployeeIdAndIdNotAndStatusAndStartOnLessThanEqualAndEndOnGreaterThanEqual(
        Long employeeId, Long excludedId, LeaveRequest.Status status, LocalDate endOn, LocalDate startOn);
}

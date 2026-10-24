package com.ems.employmenthistory.repository;

import com.ems.employmenthistory.entity.EmploymentHistory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmploymentHistoryRepository extends JpaRepository<EmploymentHistory, Long> {
    Page<EmploymentHistory> findByEmployeeIdAndKind(Long employeeId, EmploymentHistory.Kind kind, Pageable pageable);
    boolean existsByEmployeeId(Long employeeId);
    Page<EmploymentHistory> findByEmployeeId(Long employeeId, Pageable pageable);
}

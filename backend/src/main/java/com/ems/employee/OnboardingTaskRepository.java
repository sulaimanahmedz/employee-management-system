package com.ems.employee;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingTaskRepository extends JpaRepository<OnboardingTask, Long> {
    Page<OnboardingTask> findByEmployeeId(Long employeeId, Pageable pageable);
    Optional<OnboardingTask> findByIdAndEmployeeId(Long id, Long employeeId);
    long countByEmployeeIdAndCompletedTrue(Long employeeId);
}

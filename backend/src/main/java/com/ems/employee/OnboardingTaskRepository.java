package com.ems.employee;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingTaskRepository extends JpaRepository<OnboardingTask, Long> {
    Page<OnboardingTask> findByEmployeeId(Long employeeId, Pageable pageable);
    Optional<OnboardingTask> findByIdAndEmployeeId(Long id, Long employeeId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select task from OnboardingTask task where task.id = :taskId and task.employee.id = :employeeId")
    Optional<OnboardingTask> findForUpdate(@Param("taskId") Long taskId, @Param("employeeId") Long employeeId);
    long countByEmployeeIdAndCompletedTrue(Long employeeId);
}

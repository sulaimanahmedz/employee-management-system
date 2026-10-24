package com.ems.employee;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<EmployeeDocument> {
    org.springframework.data.domain.Page<EmployeeDocument> findByEmployeeId(Long employeeId, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<EmployeeDocument> findByEmployeeIdAndExpiresOnLessThanEqual(Long employeeId, java.time.LocalDate expiresBy, org.springframework.data.domain.Pageable pageable);
    List<EmployeeDocument> findByEmployeeIdOrderByIdDesc(Long employeeId);
    Optional<EmployeeDocument> findByIdAndEmployeeId(Long id, Long employeeId);
}

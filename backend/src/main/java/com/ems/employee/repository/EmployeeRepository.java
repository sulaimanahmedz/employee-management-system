package com.ems.employee.repository;

import com.ems.employee.entity.Employee;
import com.ems.employee.entity.EmployeeStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {
    long countByStatus(EmployeeStatus status);

    @Query("select count(distinct e.department) from Employee e")
    long countDepartments();
}

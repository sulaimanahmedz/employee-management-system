package com.ems.identity.entity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Column;
@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"manager_id","employee_id"}))
public class TeamMembership {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="manager_id",nullable=false) private Long managerId;
    @Column(name="employee_id",nullable=false) private Long employeeId;
    protected TeamMembership() {}
    public TeamMembership(Long manager,Long employee) { managerId=manager;employeeId=employee; }
    public Long getEmployeeId() { return employeeId; }
}

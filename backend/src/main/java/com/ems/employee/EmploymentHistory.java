package com.ems.employee;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "employment_history", indexes = @Index(name = "idx_history_employee", columnList = "employee_id,id"))
public class EmploymentHistory {
    public enum Kind { CREATED, BASELINE, CHANGED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Employee employee;
    @Column(nullable = false) private Instant recordedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Kind kind;
    @Column(nullable = false, length = 100) private String department;
    @Column(nullable = false, length = 100) private String jobTitle;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private EmployeeStatus status;
    protected EmploymentHistory() {}
    public EmploymentHistory(Employee employee, Kind kind) {
        this.employee = employee;
        this.kind = kind;
        recordedAt = Instant.now();
        department = employee.getDepartment();
        jobTitle = employee.getJobTitle();
        status = employee.getStatus();
    }
    public Long getId() { return id; }
    public Instant getRecordedAt() { return recordedAt; }
    public Kind getKind() { return kind; }
    public String getDepartment() { return department; }
    public String getJobTitle() { return jobTitle; }
    public EmployeeStatus getStatus() { return status; }
}

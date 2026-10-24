package com.ems.employee;

import jakarta.persistence.*;
import java.time.LocalDate;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "leave_requests", indexes = @Index(name = "idx_leave_employee", columnList = "employee_id,id"))
public class LeaveRequest {
    public enum Type { ANNUAL, PERSONAL, UNPAID }
    public enum Status { PENDING, CANCELLED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE) private Employee employee;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Type type;
    @Column(nullable = false) private LocalDate startOn;
    @Column(nullable = false) private LocalDate endOn;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status;
    protected LeaveRequest() {}
    public LeaveRequest(Employee employee, LeaveRequestInput input) {
        this.employee = employee; type = input.type(); startOn = input.startOn(); endOn = input.endOn(); status = Status.PENDING;
    }
    public void cancel() { status = Status.CANCELLED; }
    public Long getId() { return id; }
    public Type getType() { return type; }
    public LocalDate getStartOn() { return startOn; }
    public LocalDate getEndOn() { return endOn; }
    public Status getStatus() { return status; }
}

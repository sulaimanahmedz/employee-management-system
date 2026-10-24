package com.ems.onboarding.entity;

import com.ems.employee.entity.Employee;

import jakarta.persistence.*;
import java.time.LocalDate;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "onboarding_tasks", indexes = @Index(name = "idx_onboarding_employee", columnList = "employee_id,id"))
public class OnboardingTask {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE) private Employee employee;
    @Column(nullable = false, length = 150) private String title;
    @Column(nullable = false, length = 100) private String owner;
    private LocalDate dueOn;
    @Column(nullable = false) private boolean completed;
    protected OnboardingTask() {}
    public OnboardingTask(Employee employee, String title, String owner, LocalDate dueOn) {
        this.employee = employee; updateDetails(title, owner, dueOn);
    }
    public void updateDetails(String title, String owner, LocalDate dueOn) {
        this.title = title.strip(); this.owner = owner.strip(); this.dueOn = dueOn;
    }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getOwner() { return owner; }
    public LocalDate getDueOn() { return dueOn; }
    public boolean isCompleted() { return completed; }
}

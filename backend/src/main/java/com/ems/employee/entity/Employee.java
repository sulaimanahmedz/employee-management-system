package com.ems.employee.entity;

import jakarta.persistence.*;
import java.util.Locale;

@Entity
@Table(name = "employees", uniqueConstraints = @UniqueConstraint(name = "uk_employee_email", columnNames = "email"))
public class Employee {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100) private String firstName;
    @Column(nullable = false, length = 100) private String lastName;
    @Column(nullable = false, length = 254) private String email;
    @Column(nullable = false, length = 100) private String department;
    @Column(nullable = false, length = 100) private String jobTitle;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private EmployeeStatus status;

    protected Employee() {}
    public Employee(String firstName, String lastName, String email, String department, String jobTitle, EmployeeStatus status) {
        update(firstName, lastName, email, department, jobTitle, status);
    }
    public void update(String firstName, String lastName, String email, String department, String jobTitle, EmployeeStatus status) {
        this.firstName = firstName.strip();
        this.lastName = lastName.strip();
        this.email = email.strip().toLowerCase(Locale.ROOT);
        this.department = department.strip();
        this.jobTitle = jobTitle.strip();
        this.status = status;
    }
    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getDepartment() { return department; }
    public String getJobTitle() { return jobTitle; }
    public EmployeeStatus getStatus() { return status; }
}

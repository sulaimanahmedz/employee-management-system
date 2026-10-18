package com.ems.employee;

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
    public Employee(EmployeeRequest request) { update(request); }
    public void update(EmployeeRequest request) {
        firstName = request.firstName().strip();
        lastName = request.lastName().strip();
        email = request.email().strip().toLowerCase(Locale.ROOT);
        department = request.department().strip();
        jobTitle = request.jobTitle().strip();
        status = request.status();
    }
    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getDepartment() { return department; }
    public String getJobTitle() { return jobTitle; }
    public EmployeeStatus getStatus() { return status; }
}

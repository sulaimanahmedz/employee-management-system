package com.ems.employee;

import jakarta.persistence.*;
import java.time.LocalDate;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "employee_documents")
public class EmployeeDocument {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Employee employee;
    @Column(nullable = false, length = 150) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private Category category;
    private LocalDate expiresOn;
    public enum Category { CONTRACT, CERTIFICATE, OTHER }
    protected EmployeeDocument() {}
    public EmployeeDocument(Employee employee, DocumentRequest request) {
        this.employee = employee;
        update(request);
    }
    public void update(DocumentRequest request) {
        this.name = request.name().strip();
        this.category = request.category();
        this.expiresOn = request.expiresOn();
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public Category getCategory() { return category; }
    public LocalDate getExpiresOn() { return expiresOn; }
}

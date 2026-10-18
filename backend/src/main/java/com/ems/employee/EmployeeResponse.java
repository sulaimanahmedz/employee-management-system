package com.ems.employee;

public record EmployeeResponse(Long id, String firstName, String lastName, String email,
                               String department, String jobTitle, EmployeeStatus status) {
    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(employee.getId(), employee.getFirstName(), employee.getLastName(),
            employee.getEmail(), employee.getDepartment(), employee.getJobTitle(), employee.getStatus());
    }
}

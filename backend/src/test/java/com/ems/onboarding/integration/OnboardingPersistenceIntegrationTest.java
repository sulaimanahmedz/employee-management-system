package com.ems.onboarding.integration;

import com.ems.employee.entity.Employee;
import com.ems.employee.entity.EmployeeStatus;
import com.ems.employee.repository.EmployeeRepository;
import com.ems.onboarding.entity.OnboardingTask;
import com.ems.onboarding.repository.OnboardingTaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class OnboardingPersistenceIntegrationTest {
    @Autowired EmployeeRepository employees;
    @Autowired OnboardingTaskRepository tasks;
    @Autowired TestEntityManager entityManager;

    @Test void employeeDeletionCascadesTasksWithoutAffectingAnotherEmployee() {
        var first = employees.saveAndFlush(new Employee("One", "Test", "one@example.com", "IT", "Engineer", EmployeeStatus.ACTIVE));
        var second = employees.saveAndFlush(new Employee("Two", "Test", "two@example.com", "IT", "Engineer", EmployeeStatus.ACTIVE));
        var firstTask = tasks.saveAndFlush(new OnboardingTask(first, "Laptop", "IT", null));
        var secondTask = tasks.saveAndFlush(new OnboardingTask(second, "Orientation", "HR", null));
        assertTrue(tasks.findByIdAndEmployeeId(firstTask.getId(), second.getId()).isEmpty());
        employees.delete(first);
        employees.flush();
        entityManager.clear();
        assertFalse(tasks.existsById(firstTask.getId()));
        assertTrue(tasks.existsById(secondTask.getId()));
        assertEquals(1, tasks.countByEmployeeId(second.getId()));
    }
}

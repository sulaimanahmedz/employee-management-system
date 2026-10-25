package com.ems.onboarding;

import com.ems.employee.entity.Employee;
import com.ems.employee.entity.EmployeeStatus;
import com.ems.onboarding.entity.OnboardingTask;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OnboardingTaskUnitTest {
    @Test void normalizesDetailsAndDefaultsToPending() {
        var employee = new Employee("Synthetic", "Person", "unit@example.com", "IT", "Engineer", EmployeeStatus.ACTIVE);
        var dueOn = LocalDate.of(2026, 12, 1);
        var task = new OnboardingTask(employee, "  Laptop  ", "  IT  ", dueOn);
        assertEquals("Laptop", task.getTitle());
        assertEquals("IT", task.getOwner());
        assertEquals(dueOn, task.getDueOn());
        assertFalse(task.isCompleted());
        assertNull(task.getId());
    }

    @Test void editingDetailsPreservesCompletionAndCanClearDueDate() {
        var employee = new Employee("Synthetic", "Person", "unit@example.com", "IT", "Engineer", EmployeeStatus.ACTIVE);
        var task = new OnboardingTask(employee, "Laptop", "IT", LocalDate.of(2026, 12, 1));
        task.setCompleted(true);
        task.updateDetails("  Orientation  ", "  People  ", null);
        assertEquals("Orientation", task.getTitle());
        assertEquals("People", task.getOwner());
        assertNull(task.getDueOn());
        assertTrue(task.isCompleted());
        task.setCompleted(false);
        assertFalse(task.isCompleted());
        assertEquals("Orientation", task.getTitle());
    }
}

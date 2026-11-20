package com.ems.onboarding.unit;
import com.ems.employee.entity.Employee;
import com.ems.employee.repository.EmployeeRepository;
import com.ems.onboarding.repository.OnboardingTaskRepository;
import com.ems.onboarding.service.OnboardingTaskService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class OnboardingSearchTest {
    @Test void oversizedSearchIsRejectedBeforeTaskQueries() {
        var employees=mock(EmployeeRepository.class);var tasks=mock(OnboardingTaskRepository.class);
        when(employees.findById(1L)).thenReturn(Optional.of(mock(Employee.class)));
        var service=new OnboardingTaskService(employees,tasks);
        assertEquals(400,assertThrows(ResponseStatusException.class,()->service.list(1L,0,null,null,"x".repeat(151))).getStatusCode().value());
        verifyNoInteractions(tasks);
    }
}

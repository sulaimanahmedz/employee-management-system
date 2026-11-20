package com.ems.onboarding.integration;
import com.ems.employee.entity.Employee;
import com.ems.employee.entity.EmployeeStatus;
import com.ems.employee.repository.EmployeeRepository;
import com.ems.onboarding.entity.OnboardingTask;
import com.ems.onboarding.repository.OnboardingTaskRepository;
import com.ems.onboarding.service.OnboardingTaskService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:onboarding-search;DB_CLOSE_DELAY=-1")
class OnboardingSearchIntegrationTest {
    @Autowired EmployeeRepository employees; @Autowired OnboardingTaskRepository tasks; @Autowired OnboardingTaskService service;
    @Test void searchIsLiteralScopedAndCombinedBeforePaginationWithIndependentProgress() {
        var e=employees.saveAndFlush(new Employee("Synthetic","Search","search@example.com","IT","Engineer",EmployeeStatus.ACTIVE));
        var other=employees.saveAndFlush(new Employee("Other","Search","other-search@example.com","IT","Engineer",EmployeeStatus.ACTIVE));
        var date=LocalDate.of(2026,12,1);
        for(int i=0;i<11;i++)tasks.saveAndFlush(new OnboardingTask(e,"Prepare laptop "+i,"IT team",date));
        var done=new OnboardingTask(e,"Orientation","People team",null);done.setCompleted(true);tasks.saveAndFlush(done);
        tasks.saveAndFlush(new OnboardingTask(e,"100%_literal","Facilities",date));
        tasks.saveAndFlush(new OnboardingTask(other,"Prepare laptop foreign","IT team",date));
        var page=service.list(e.getId(),0,false,date,"  LAPTOP  ");
        assertEquals(11,page.totalElements());assertEquals(10,page.items().size());assertEquals(13,page.totalTasks());assertEquals(1,page.completedTasks());
        var next=service.list(e.getId(),1,false,date,"laptop");assertEquals(1,next.items().size());assertTrue(page.items().stream().noneMatch(t->t.id().equals(next.items().get(0).id())));
        assertEquals(11,service.list(e.getId(),0,null,null,"it TEAM").totalElements());
        assertEquals(1,service.list(e.getId(),0,null,null,"%_").totalElements());
        assertEquals(0,service.list(e.getId(),0,true,date,"laptop").totalElements());
        assertEquals(13,service.list(e.getId(),0,null,null,"   ").totalElements());
        assertEquals(0,service.list(e.getId(),0,null,null,"z".repeat(150)).totalElements());
    }
}

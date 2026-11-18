package com.ems.identity.integration;

import com.ems.employee.dto.EmployeeRequest;
import com.ems.employee.entity.EmployeeStatus;
import com.ems.employee.service.EmployeeService;
import com.ems.identity.entity.AccountRole;
import com.ems.identity.service.AccountService;
import com.ems.identity.service.AccountScopeService;
import com.ems.identity.service.AccountLifecycleService;
import com.ems.identity.repository.SecurityEventRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:scope-integration;DB_CLOSE_DELAY=-1")
class AccountScopeIntegrationTest {
    @Autowired AccountService accounts;
    @Autowired AccountScopeService scopes;
    @Autowired AccountLifecycleService lifecycle;
    @Autowired EmployeeService employees;
    @Autowired SecurityEventRepository events;
    @Test void assignedAccessIsExplicitTransactionalUniqueAndRemovedOnRoleChanges() {
        var manager=accounts.provision("scope-manager","Synthetic scope password!",AccountRole.MANAGER);
        var user=accounts.provision("scope-employee","Synthetic scope password!",AccountRole.EMPLOYEE);
        var own=employees.create(new EmployeeRequest("Synthetic","Owner","scope-owner@example.com","Test","Analyst",EmployeeStatus.ACTIVE));
        var team=employees.create(new EmployeeRequest("Synthetic","Team","scope-team@example.com","Test","Analyst",EmployeeStatus.ACTIVE));
        assertTrue(scopes.permittedIds(manager.id()).isEmpty());
        assertThrows(ResponseStatusException.class,()->scopes.read(manager.id(),own.id()));
        scopes.replace(99L,manager.id(),own.id(),List.of(team.id(),team.id()));
        assertEquals(List.of(own.id(),team.id()),scopes.permittedIds(manager.id()));
        assertEquals(team.email(),scopes.read(manager.id(),team.id()).email());
        assertEquals(List.of(team.id()),scopes.scope(manager.id()).teamEmployeeIds());
        assertThrows(ResponseStatusException.class,()->scopes.replace(99L,user.id(),own.id(),List.of()));
        assertThrows(ResponseStatusException.class,()->scopes.replace(99L,user.id(),null,List.of(team.id())));
        assertThrows(RuntimeException.class,()->scopes.replace(99L,manager.id(),own.id(),List.of(Long.MAX_VALUE)));
        assertEquals(List.of(team.id()),scopes.scope(manager.id()).teamEmployeeIds(),"Failed replacements roll back all grants");
        scopes.replace(99L,user.id(),team.id(),List.of());
        assertEquals(List.of(team.id()),scopes.permittedIds(user.id()));
        assertThrows(ResponseStatusException.class,()->scopes.read(user.id(),own.id()));
        lifecycle.changeAccess(99L,manager.id(),AccountRole.EMPLOYEE,true);
        assertEquals(List.of(own.id()),scopes.permittedIds(manager.id()));
        lifecycle.changeAccess(99L,manager.id(),AccountRole.MANAGER,true);
        assertTrue(scopes.scope(manager.id()).teamEmployeeIds().isEmpty());
        scopes.replace(99L,manager.id(),null,List.of());
        assertTrue(scopes.permittedIds(manager.id()).isEmpty());
        assertTrue(events.count()>=8);
        assertThrows(ResponseStatusException.class,()->scopes.scope(Long.MAX_VALUE));
        assertThrows(ResponseStatusException.class,()->scopes.replace(99L,Long.MAX_VALUE,null,List.of()));
        assertThrows(ResponseStatusException.class,()->scopes.permittedIds(Long.MAX_VALUE));
    }
}

package com.ems.identity.controller;
import com.ems.identity.dto.ScopeInput;
import com.ems.identity.dto.ScopeView;
import com.ems.identity.dto.SessionPrincipal;
import com.ems.identity.service.AccountScopeService;
import com.ems.employee.dto.EmployeeResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class AccountScopeController {
    private final AccountScopeService scopes;
    public AccountScopeController(AccountScopeService scopes) { this.scopes=scopes; }
    @GetMapping("/api/v1/admin/accounts/{id}/scope")
    public ScopeView scope(@PathVariable Long id) { return scopes.scope(id); }
    @PutMapping("/api/v1/admin/accounts/{id}/scope")
    public ScopeView replace(Authentication auth,@PathVariable Long id,@Valid @RequestBody ScopeInput input) {
        return scopes.replace(((SessionPrincipal)auth.getPrincipal()).accountId(),id,input.employeeId(),input.teamEmployeeIds());
    }
    @GetMapping("/api/v1/self/employees")
    public List<Long> permitted(Authentication auth) { return scopes.permittedIds(((SessionPrincipal)auth.getPrincipal()).accountId()); }
    @GetMapping("/api/v1/self/employees/{id}")
    public EmployeeResponse employee(Authentication auth,@PathVariable Long id) { return scopes.read(((SessionPrincipal)auth.getPrincipal()).accountId(),id); }
}

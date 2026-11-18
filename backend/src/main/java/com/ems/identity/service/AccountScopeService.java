package com.ems.identity.service;
import com.ems.identity.dto.ScopeView;
import com.ems.identity.entity.AccountRole;
import com.ems.identity.entity.SecurityEvent;
import com.ems.identity.entity.TeamMembership;
import com.ems.identity.repository.AccountRepository;
import com.ems.identity.repository.TeamMembershipRepository;
import com.ems.employee.service.EmployeeService;
import com.ems.employee.dto.EmployeeResponse;
import java.util.List;
import java.util.LinkedHashSet;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AccountScopeService {
    private final AccountRepository accounts;
    private final TeamMembershipRepository teams;
    private final EmployeeService employees;
    private final AuditTrailService audit;
    public AccountScopeService(AccountRepository accounts,TeamMembershipRepository teams,EmployeeService employees,AuditTrailService audit) {
        this.accounts=accounts;this.teams=teams;this.employees=employees;this.audit=audit;
    }
    @Transactional(readOnly=true) public ScopeView scope(Long id) {
        var account=accounts.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new ScopeView(account.getEmployeeId(),teams.findByManagerIdOrderByEmployeeId(id).stream().map(TeamMembership::getEmployeeId).toList());
    }
    public ScopeView replace(Long actor,Long id,Long employee,List<Long> members) {
        var locked=accounts.lockAll();
        var target=locked.stream().filter(a -> a.getId().equals(id)).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!members.isEmpty() && target.getRole()!=AccountRole.MANAGER) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only managers can have a team");
        if (employee!=null) {
            if (locked.stream().anyMatch(a -> !a.getId().equals(id) && employee.equals(a.getEmployeeId())))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Employee is already linked to an account");
            employees.get(employee);
        }
        var unique=new LinkedHashSet<>(members);
        unique.forEach(employees::get);
        target.linkEmployee(employee);
        teams.deleteByManagerId(id); teams.flush();
        unique.forEach(member -> teams.save(new TeamMembership(id,member)));
        audit.record(SecurityEvent.Action.LINK_CHANGED,actor,id);
        audit.record(SecurityEvent.Action.TEAM_CHANGED,actor,id);
        return new ScopeView(employee,List.copyOf(unique));
    }
    @Transactional(readOnly=true) public List<Long> permittedIds(Long id) {
        var account=accounts.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        var ids=new LinkedHashSet<Long>();
        if (account.getEmployeeId()!=null) ids.add(account.getEmployeeId());
        if (account.getRole()==AccountRole.MANAGER) teams.findByManagerIdOrderByEmployeeId(id).forEach(m -> ids.add(m.getEmployeeId()));
        return List.copyOf(ids);
    }
    @Transactional(readOnly=true) public EmployeeResponse read(Long id,Long employee) {
        if (!permittedIds(id).contains(employee)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return employees.get(employee);
    }
}

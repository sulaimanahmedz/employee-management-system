package com.ems.identity.controller;

import com.ems.identity.dto.AccountAccessInput;
import com.ems.identity.dto.AccountAccessView;
import com.ems.identity.dto.PasswordChangeInput;
import com.ems.identity.dto.ResetInput;
import com.ems.identity.dto.ResetTicket;
import com.ems.identity.dto.SessionPrincipal;
import com.ems.identity.dto.SecurityEventView;
import com.ems.identity.entity.SecurityEvent;
import com.ems.identity.service.AccountLifecycleService;
import com.ems.identity.service.AuditTrailService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountLifecycleController {
    private final AccountLifecycleService accounts;
    private final AuditTrailService audit;
    public AccountLifecycleController(AccountLifecycleService accounts, AuditTrailService audit) { this.accounts=accounts; this.audit=audit; }
    @PostMapping("/api/v1/auth/password") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void password(Authentication auth, @Valid @RequestBody PasswordChangeInput input) {
        accounts.changePassword(auth.getName(),input.currentPassword(),input.newPassword());
    }
    @GetMapping("/api/v1/admin/accounts/{id}")
    public AccountAccessView get(@PathVariable Long id) { return accounts.get(id); }
    @PutMapping("/api/v1/admin/accounts/{id}")
    public AccountAccessView access(Authentication auth,@PathVariable Long id,@Valid @RequestBody AccountAccessInput input) {
        return accounts.changeAccess(((SessionPrincipal)auth.getPrincipal()).accountId(),id,input.role(),input.enabled());
    }
    @PostMapping("/api/v1/admin/accounts/{id}/reset")
    public ResetTicket issue(Authentication auth,@PathVariable Long id) {
        return accounts.issueReset(((SessionPrincipal)auth.getPrincipal()).accountId(),id);
    }
    @PostMapping("/api/v1/auth/reset") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody ResetInput input) { accounts.reset(input.token(),input.newPassword()); }
    @GetMapping("/api/v1/admin/audit")
    public List<SecurityEventView> events(@RequestParam(defaultValue="0") int page,
            @RequestParam(required=false) SecurityEvent.Action action, @RequestParam(required=false) Long subjectId) { return audit.list(page, action, subjectId); }
    @ExceptionHandler({IllegalArgumentException.class,BadCredentialsException.class})
    public ProblemDetail invalid(RuntimeException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Unable to complete account operation");
    }
}

package com.ems.identity.service;

import com.ems.identity.dto.SessionPrincipal;
import com.ems.identity.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionValidityService {
    private final AccountRepository accounts;
    public SessionValidityService(AccountRepository accounts) { this.accounts = accounts; }
    @Transactional(readOnly = true)
    public boolean valid(SessionPrincipal principal) {
        return accounts.findById(principal.accountId()).filter(account -> account.isEnabled()
            && account.getSecurityVersion() == principal.securityVersion()
            && account.getLogin().equals(principal.login())).isPresent();
    }
}

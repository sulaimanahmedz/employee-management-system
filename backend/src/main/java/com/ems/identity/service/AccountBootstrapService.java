package com.ems.identity.service;

import com.ems.identity.entity.AccountRole;
import com.ems.identity.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

/** Explicit operator bootstrap, only for an empty account store; never logs credentials. */
@Service
public class AccountBootstrapService implements ApplicationRunner {
    private final AccountService accounts;
    private final AccountRepository repository;
    private final String login;
    private final String password;
    public AccountBootstrapService(AccountService accounts, AccountRepository repository,
            @Value("${EMS_BOOTSTRAP_LOGIN:}") String login,
            @Value("${EMS_BOOTSTRAP_PASSWORD:}") String password) {
        this.accounts = accounts; this.repository = repository; this.login = login; this.password = password;
    }
    @Override public void run(ApplicationArguments arguments) {
        if (login.isEmpty() && password.isEmpty()) return;
        if (login.isEmpty() || password.isEmpty()) throw new IllegalStateException("Both bootstrap credentials are required");
        if (repository.count() != 0) return;
        accounts.provision(login, password, AccountRole.ADMIN);
    }
}

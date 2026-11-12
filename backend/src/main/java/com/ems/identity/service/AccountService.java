package com.ems.identity.service;

import com.ems.identity.dto.AccountIdentity;
import com.ems.identity.dto.AccountPage;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.ems.identity.entity.Account;
import com.ems.identity.entity.AccountRole;
import com.ems.identity.repository.AccountRepository;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Credential and provisioning transactions; HTTP access is enforced by the identity security chain. */
@Service
@Transactional
public class AccountService {
    private final AccountRepository accounts;
    private final PasswordHashService passwords;
    private final String missingAccountHash;

    public AccountService(AccountRepository accounts, PasswordHashService passwords) {
        this.accounts = accounts;
        this.passwords = passwords;
        // Run a real password comparison even when the login is absent.
        this.missingAccountHash = passwords.hash(UUID.randomUUID().toString());
    }

    public AccountIdentity provision(String login, String password, AccountRole role) {
        String normalized = normalize(login);
        if (!normalized.matches("[a-z0-9][a-z0-9._-]{2,63}")) {
            throw new IllegalArgumentException("Login must contain 3 to 64 ASCII letters, digits, dots, underscores or hyphens and start with a letter or digit");
        }
        Objects.requireNonNull(role, "Account role is required");
        var saved = accounts.saveAndFlush(new Account(normalized, passwords.hash(password), role));
        return identity(saved);
    }

    @Transactional(readOnly = true)
    public AccountPage list(int page) {
        if (page < 0 || page > Integer.MAX_VALUE / 10) {
            throw new IllegalArgumentException("Account page must be nonnegative and its offset fit a signed integer");
        }
        var result = accounts.findAll(PageRequest.of(page, 10, Sort.by("id").ascending()));
        return new AccountPage(result.map(AccountService::identity).getContent(), page,
            result.getTotalPages(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Optional<AccountIdentity> verifyCredentials(String login, String password) {
        var account = accounts.findByLogin(normalize(login));
        boolean matches = passwords.matches(password, account.map(Account::getPasswordHash).orElse(missingAccountHash));
        if (!matches) return Optional.empty();
        return account.map(AccountService::identity);
    }

    private static String normalize(String login) {
        return login == null ? "" : login.strip().toLowerCase(Locale.ROOT);
    }

    private static AccountIdentity identity(Account account) {
        return new AccountIdentity(account.getId(), account.getLogin(), account.getRole());
    }
}

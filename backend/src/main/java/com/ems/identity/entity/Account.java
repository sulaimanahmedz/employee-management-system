package com.ems.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Credential storage only: never expose this entity in an HTTP response. */
@Entity
@Table(name = "identity_accounts", uniqueConstraints = @UniqueConstraint(name = "uk_account_login", columnNames = "login"))
public class Account {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 64)
    private String login;
    @Column(nullable = false, length = 60)
    private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private AccountRole role;

    @Column(nullable = false)
    private boolean enabled = true;
    @Column(nullable = false)
    private long securityVersion;

    @Column(length = 64, unique = true)
    private String resetDigest;
    private java.time.Instant resetExpiresAt;
    public String getResetDigest() { return resetDigest; }
    public java.time.Instant getResetExpiresAt() { return resetExpiresAt; }
    public void issueReset(String digest, java.time.Instant expiry) { resetDigest = digest; resetExpiresAt = expiry; }
    public void clearReset() { resetDigest = null; resetExpiresAt = null; }
    @Column(unique = true)
    private Long employeeId;
    public Long getEmployeeId() { return employeeId; }
    public void linkEmployee(Long id) { employeeId = id; securityVersion++; clearReset(); }
    public boolean isEnabled() { return enabled; }
    public long getSecurityVersion() { return securityVersion; }
    public void changePassword(String hash) { passwordHash = hash; securityVersion++; clearReset(); }
    public void changeAccess(AccountRole nextRole, boolean active) {
        role = nextRole; enabled = active; securityVersion++; clearReset();
    }

    protected Account() {}

    public Account(String login, String passwordHash, AccountRole role) {
        this.login = login;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getLogin() { return login; }
    public String getPasswordHash() { return passwordHash; }
    public AccountRole getRole() { return role; }
}

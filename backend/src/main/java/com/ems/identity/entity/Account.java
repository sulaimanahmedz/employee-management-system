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

package com.ems.identity.integration;

import com.ems.identity.entity.Account;
import com.ems.identity.entity.AccountRole;
import com.ems.identity.repository.AccountRepository;
import com.ems.identity.service.AccountService;
import com.ems.identity.service.PasswordHashService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({AccountService.class, PasswordHashService.class})
class AccountPersistenceIntegrationTest {
    @Autowired AccountService service;
    @Autowired AccountRepository accounts;
    @Autowired TestEntityManager entityManager;

    @Test void roundTripsAllRolesWithoutStoringOrReturningCredentialsInIdentity() {
        for (AccountRole role : AccountRole.values()) {
            String login = "synthetic." + role.name().toLowerCase(java.util.Locale.ROOT);
            var identity = service.provision(login, "Synthetic password!", role);
            assertNotNull(identity.id());
            entityManager.clear();
            Account persisted = accounts.findByLogin(login).orElseThrow();
            assertEquals(identity.id(), persisted.getId());
            assertEquals(role, persisted.getRole());
            assertNotEquals("Synthetic password!", persisted.getPasswordHash());
            assertEquals(identity, service.verifyCredentials(login.toUpperCase(java.util.Locale.ROOT), "Synthetic password!").orElseThrow());
            assertTrue(service.verifyCredentials(login, "Invalid password!").isEmpty());
            assertFalse(identity.toString().contains(persisted.getPasswordHash()));
            assertFalse(identity.toString().contains("Synthetic password!"));
        }
        assertTrue(service.verifyCredentials("unknown.synthetic", "Synthetic password!").isEmpty());
    }

    @Test void databaseEnforcesCaseNormalizedLoginUniqueness() {
        service.provision("Synthetic.Login", "Synthetic password!", AccountRole.ADMIN);
        assertThrows(DataIntegrityViolationException.class,
            () -> service.provision(" SYNTHETIC.LOGIN ", "Other synthetic password!", AccountRole.HR));
    }

    @Test void databaseRejectsDuplicateLoginWithoutAnApplicationPrecheck() {
        accounts.saveAndFlush(new Account("synthetic.login", "encoded-value", AccountRole.MANAGER));
        assertThrows(DataIntegrityViolationException.class,
            () -> accounts.saveAndFlush(new Account("synthetic.login", "another-value", AccountRole.EMPLOYEE)));
    }
    @Test void directoryPagesPersistedIdentitiesWithoutOverlapOrCredentials() {
        var ids = new java.util.ArrayList<Long>();
        for (int i = 0; i < 12; i++) {
            ids.add(accounts.saveAndFlush(new Account("synthetic-page-" + i, "not-a-real-hash", AccountRole.EMPLOYEE)).getId());
        }
        entityManager.clear();
        var first = service.list(0);
        var second = service.list(1);
        assertEquals(ids.subList(0, 10), first.items().stream().map(com.ems.identity.dto.AccountIdentity::id).toList());
        assertEquals(ids.subList(10, 12), second.items().stream().map(com.ems.identity.dto.AccountIdentity::id).toList());
        assertEquals(12, first.totalElements());
        assertEquals(2, second.totalPages());
        assertTrue(service.list(2).items().isEmpty());
        assertTrue(service.list(Integer.MAX_VALUE / 10).items().isEmpty());
        assertFalse(first.toString().contains("not-a-real-hash"));
    }
    @Test void profileReflectsStoredIdentityAndRemoval() {
        var identity = service.provision("synthetic-profile", "Synthetic profile password!", AccountRole.MANAGER);
        entityManager.clear();
        assertEquals(identity, service.findIdentity("synthetic-profile").orElseThrow());
        accounts.deleteById(identity.id()); accounts.flush(); entityManager.clear();
        assertTrue(service.findIdentity("synthetic-profile").isEmpty());
    }
}

package com.ems.identity.unit;

import com.ems.identity.entity.Account;
import com.ems.identity.entity.AccountRole;
import com.ems.identity.repository.AccountRepository;
import com.ems.identity.service.AccountService;
import com.ems.identity.service.PasswordHashService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AccountServiceTest {
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final PasswordHashService passwords = mock(PasswordHashService.class);
    private AccountService service;

    @BeforeEach void setup() {
        when(passwords.hash(anyString())).thenReturn("dummy-hash");
        service = new AccountService(accounts, passwords);
        clearInvocations(passwords);
    }

    @Test void provisionsNormalizedLoginAndNeverPersistsTheRawPassword() {
        when(passwords.hash("Synthetic password!")).thenReturn("encoded-password");
        when(accounts.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        var identity = service.provision("  HR.Example  ", "Synthetic password!", AccountRole.HR);
        var saved = ArgumentCaptor.forClass(Account.class);
        verify(accounts).saveAndFlush(saved.capture());
        assertEquals("hr.example", saved.getValue().getLogin());
        assertEquals("encoded-password", saved.getValue().getPasswordHash());
        assertEquals(AccountRole.HR, identity.role());
        assertEquals("hr.example", identity.login());
    }

    @Test void rejectsInvalidIdentifiersRolesAndPasswordsBeforePersistence() {
        for (String login : new String[]{null, "", "ab", "_abc", "a b", "a@b", "x".repeat(65), "école"}) {
            assertThrows(IllegalArgumentException.class, () -> service.provision(login, "Synthetic password!", AccountRole.ADMIN));
        }
        assertThrows(NullPointerException.class, () -> service.provision("admin", "Synthetic password!", null));
        when(passwords.hash("short")).thenThrow(new IllegalArgumentException("Invalid password"));
        assertThrows(IllegalArgumentException.class, () -> service.provision("admin", "short", AccountRole.ADMIN));
        verifyNoInteractions(accounts);
    }

    @Test void authenticatesOnlyAStoredAccountWithMatchingCredentials() {
        var account = new Account("employee.one", "saved-hash", AccountRole.EMPLOYEE);
        when(accounts.findByLogin("employee.one")).thenReturn(Optional.of(account));
        when(passwords.matches("correct", "saved-hash")).thenReturn(true);
        assertEquals(AccountRole.EMPLOYEE, service.verifyCredentials(" EMPLOYEE.ONE ", "correct").orElseThrow().role());
        assertTrue(service.verifyCredentials("employee.one", "wrong").isEmpty());
        verify(accounts, never()).saveAndFlush(any());
    }

    @Test void missingAccountsStillCompareAgainstADummyHashAndCannotAuthenticate() {
        when(accounts.findByLogin(anyString())).thenReturn(Optional.empty());
        assertTrue(service.verifyCredentials(null, "wrong").isEmpty());
        verify(passwords).matches("wrong", "dummy-hash");
        when(passwords.matches("accidental-match", "dummy-hash")).thenReturn(true);
        assertTrue(service.verifyCredentials("missing", "accidental-match").isEmpty());
    }
}

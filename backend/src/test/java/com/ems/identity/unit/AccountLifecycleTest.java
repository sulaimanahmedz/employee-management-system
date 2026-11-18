package com.ems.identity.unit;

import com.ems.identity.entity.Account;
import com.ems.identity.entity.AccountRole;
import com.ems.identity.entity.SecurityEvent;
import com.ems.identity.repository.AccountRepository;
import com.ems.identity.repository.TeamMembershipRepository;
import com.ems.identity.service.AccountLifecycleService;
import com.ems.identity.service.AuditTrailService;
import com.ems.identity.service.LoginAttemptService;
import com.ems.identity.service.PasswordHashService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountLifecycleTest {
    final AccountRepository accounts=mock(AccountRepository.class);
    final TeamMembershipRepository teams=mock(TeamMembershipRepository.class);
    final PasswordHashService passwords=mock(PasswordHashService.class);
    final LoginAttemptService attempts=mock(LoginAttemptService.class);
    final AuditTrailService audit=mock(AuditTrailService.class);
    final Instant now=Instant.parse("2026-11-17T00:00:00Z");
    final AccountLifecycleService service=new AccountLifecycleService(accounts,passwords,attempts,audit,teams,Clock.fixed(now,ZoneOffset.UTC));
    Account account(long id,AccountRole role) {
        var a=new Account("synthetic-"+id,"old-hash",role);
        ReflectionTestUtils.setField(a,"id",id);return a;
    }
    @Test void passwordRequiresCurrentSecretAndRevokesPriorSessions() {
        var a=account(1,AccountRole.EMPLOYEE);
        when(accounts.lockByLogin(a.getLogin())).thenReturn(Optional.of(a));
        when(passwords.matches("old", "old-hash")).thenReturn(true);
        when(passwords.hash("replacement")).thenReturn("new-hash");
        a.issueReset("digest",now.plusSeconds(30));
        service.changePassword(a.getLogin(),"old","replacement");
        assertEquals("new-hash",a.getPasswordHash());assertEquals(1,a.getSecurityVersion());
        assertNull(a.getResetDigest());assertNull(a.getResetExpiresAt());
        verify(audit).record(SecurityEvent.Action.PASSWORD_CHANGED,1L,1L);
        verify(attempts).succeeded(a.getLogin());verify(attempts).release();
    }
    @Test void invalidPasswordAttemptsDoNotMutateAccountAndAlwaysReleaseCapacity() {
        var a=account(1,AccountRole.HR);when(accounts.lockByLogin(a.getLogin())).thenReturn(Optional.of(a));
        assertThrows(ResponseStatusException.class,()->service.changePassword(a.getLogin(),"wrong","replacement"));
        a.changeAccess(AccountRole.HR,false);
        assertThrows(ResponseStatusException.class,()->service.changePassword(a.getLogin(),"old","replacement"));
        assertThrows(ResponseStatusException.class,()->service.changePassword("missing","old","replacement"));
        assertEquals("old-hash",a.getPasswordHash());verifyNoInteractions(audit);
        verify(attempts,times(3)).release();
    }
    @Test void lastAdministratorCannotBeDisabledOrDemotedButOtherChangesAreAllowed() {
        var a=account(1,AccountRole.ADMIN);var hr=account(2,AccountRole.HR);
        when(accounts.lockAll()).thenReturn(List.of(a,hr));
        assertThrows(ResponseStatusException.class,()->service.changeAccess(1L,1L,AccountRole.ADMIN,false));
        assertThrows(ResponseStatusException.class,()->service.changeAccess(1L,1L,AccountRole.HR,true));
        assertEquals(AccountRole.ADMIN,service.changeAccess(1L,1L,AccountRole.ADMIN,true).role());
        assertEquals(AccountRole.MANAGER,service.changeAccess(1L,2L,AccountRole.MANAGER,true).role());
        var second=account(3,AccountRole.ADMIN);var inactive=account(4,AccountRole.ADMIN);inactive.changeAccess(AccountRole.ADMIN,false);when(accounts.lockAll()).thenReturn(List.of(a,hr,second,inactive));
        assertFalse(service.changeAccess(3L,1L,AccountRole.EMPLOYEE,false).enabled());
        assertTrue(service.changeAccess(3L,1L,AccountRole.EMPLOYEE,true).enabled());
        assertThrows(ResponseStatusException.class,()->service.changeAccess(3L,9L,AccountRole.HR,true));
        verify(teams,times(3)).deleteByManagerId(1L);
    }
    @Test void resetUsesHashedExpiringSingleUseTokensAndRejectsInactiveOrMissingAccounts() {
        var a=account(1,AccountRole.HR);when(accounts.lockAll()).thenReturn(List.of(a));
        var ticket=service.issueReset(2L,1L);
        assertEquals(64,ticket.token().length());assertNotEquals(ticket.token(),a.getResetDigest());
        assertEquals(now.plusSeconds(900),ticket.expiresAt());
        String digest=a.getResetDigest();when(accounts.lockByResetDigest(digest)).thenReturn(Optional.of(a));
        when(passwords.hash("replacement")).thenReturn("new-hash");
        service.reset(ticket.token(),"replacement");
        assertNull(a.getResetDigest());assertEquals(1,a.getSecurityVersion());
        verify(audit).record(SecurityEvent.Action.PASSWORD_RESET,null,1L);
        when(accounts.lockByResetDigest(digest)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class,()->service.reset(ticket.token(),"replacement"));
        a.issueReset(digest,now);when(accounts.lockByResetDigest(digest)).thenReturn(Optional.of(a));
        assertThrows(ResponseStatusException.class,()->service.reset(ticket.token(),"replacement"));
        a.changeAccess(AccountRole.HR,false);a.issueReset(digest,now.plusSeconds(1));
        assertThrows(ResponseStatusException.class,()->service.reset(ticket.token(),"replacement"));
        assertThrows(ResponseStatusException.class,()->service.issueReset(2L,1L));
        assertThrows(ResponseStatusException.class,()->service.issueReset(2L,9L));
        verify(attempts,times(4)).release();
    }
    @Test void lookupReturnsOnlySafeAccountFieldsAndMissingIsNotFound() {
        var a=account(1,AccountRole.HR);when(accounts.findById(1L)).thenReturn(Optional.of(a));
        assertEquals(a.getLogin(),service.get(1L).login());assertEquals(1L,service.get(1L).id());
        assertThrows(ResponseStatusException.class,()->service.get(2L));
    }
    @Test void unavailableDigestFailsClosedWithoutIssuingToken() throws Exception {
        var a=account(1,AccountRole.HR);when(accounts.lockAll()).thenReturn(List.of(a));
        try(var crypto=mockStatic(java.security.MessageDigest.class)) {
            crypto.when(()->java.security.MessageDigest.getInstance("SHA-256")).thenThrow(new java.security.NoSuchAlgorithmException());
            assertThrows(IllegalStateException.class,()->service.issueReset(2L,1L));
        }
        assertNull(a.getResetDigest());verifyNoInteractions(audit);
    }
}

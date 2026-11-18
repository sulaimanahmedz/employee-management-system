package com.ems.identity.service;

import com.ems.identity.dto.AccountAccessView;
import com.ems.identity.dto.ResetTicket;
import com.ems.identity.entity.Account;
import com.ems.identity.entity.AccountRole;
import com.ems.identity.entity.SecurityEvent;
import com.ems.identity.repository.AccountRepository;
import com.ems.identity.repository.TeamMembershipRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AccountLifecycleService {
    private final AccountRepository accounts;
    private final PasswordHashService passwords;
    private final LoginAttemptService attempts;
    private final AuditTrailService audit;
    private final Clock clock;
    private final TeamMembershipRepository teams;
    @Autowired public AccountLifecycleService(AccountRepository accounts, PasswordHashService passwords,
            LoginAttemptService attempts, AuditTrailService audit, TeamMembershipRepository teams) {
        this(accounts, passwords, attempts, audit, teams, Clock.systemUTC());
    }
    public AccountLifecycleService(AccountRepository accounts, PasswordHashService passwords,
            LoginAttemptService attempts, AuditTrailService audit, TeamMembershipRepository teams, Clock clock) {
        this.accounts=accounts; this.passwords=passwords; this.attempts=attempts; this.audit=audit; this.clock=clock; this.teams=teams;
    }
    public void changePassword(String login, String current, String replacement) {
        attempts.acquire(login);
        try {
            var account=accounts.lockByLogin(login).orElseThrow(AccountLifecycleService::rejected);
            if (!account.isEnabled() || !passwords.matches(current, account.getPasswordHash())) throw rejected();
            account.changePassword(passwords.hash(replacement));
            audit.record(SecurityEvent.Action.PASSWORD_CHANGED, account.getId(), account.getId());
            attempts.succeeded(login);
        } finally { attempts.release(); }
    }
    @Transactional(readOnly=true) public AccountAccessView get(Long id) {
        return view(accounts.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }
    public AccountAccessView changeAccess(Long actor, Long id, AccountRole role, boolean enabled) {
        var locked=accounts.lockAll();
        var target=locked.stream().filter(a -> a.getId().equals(id)).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (target.isEnabled() && target.getRole()==AccountRole.ADMIN && (!enabled || role!=AccountRole.ADMIN)
                && locked.stream().filter(a -> a.isEnabled() && a.getRole()==AccountRole.ADMIN).count()==1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "At least one active administrator is required");
        }
        target.changeAccess(role,enabled);
        if (role!=AccountRole.MANAGER) teams.deleteByManagerId(id);
        audit.record(SecurityEvent.Action.ACCOUNT_ACCESS_CHANGED,actor,id);
        return view(target);
    }
    public ResetTicket issueReset(Long actor, Long id) {
        var target=accounts.lockAll().stream().filter(a -> a.getId().equals(id)).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!target.isEnabled()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Account is inactive");
        byte[] bytes=new byte[32]; new SecureRandom().nextBytes(bytes);
        String token=HexFormat.of().formatHex(bytes); var expiry=clock.instant().plusSeconds(900);
        target.issueReset(digest(token),expiry);
        audit.record(SecurityEvent.Action.RESET_ISSUED,actor,id);
        return new ResetTicket(token,expiry);
    }
    public void reset(String token, String replacement) {
        // A single bounded bucket prevents unbounded token-key storage and limits online guesses.
        attempts.acquire("<password-reset>");
        try {
            var target=accounts.lockByResetDigest(digest(token)).orElseThrow(AccountLifecycleService::rejected);
            if (!target.isEnabled() || !clock.instant().isBefore(target.getResetExpiresAt())) throw rejected();
            target.changePassword(passwords.hash(replacement));
            audit.record(SecurityEvent.Action.PASSWORD_RESET,null,target.getId());
        } finally { attempts.release(); }
    }
    private static ResponseStatusException rejected() { return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unable to complete password operation"); }
    private static AccountAccessView view(Account a) { return new AccountAccessView(a.getId(),a.getLogin(),a.getRole(),a.isEnabled()); }
    private static String digest(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException("SHA-256 unavailable",impossible); }
    }
}

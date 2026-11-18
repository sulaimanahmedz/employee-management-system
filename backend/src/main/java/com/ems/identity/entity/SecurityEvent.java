package com.ems.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class SecurityEvent {
    public enum Action { PASSWORD_CHANGED, ACCOUNT_ACCESS_CHANGED, RESET_ISSUED, PASSWORD_RESET, LINK_CHANGED, TEAM_CHANGED,
        CREDENTIALS_ACCEPTED, CREDENTIALS_REJECTED, VERIFICATION_UNAVAILABLE, SESSION_ESTABLISHED, LOGOUT_PROCESSED,
        AUTHENTICATION_REQUIRED, ACCESS_DENIED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Instant occurredAt;
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING) @Column(nullable = false, length = 40) private Action action;
    private Long actorId;
    private Long subjectId;
    protected SecurityEvent() {}
    public SecurityEvent(Instant time, Action action, Long actor, Long subject) {
        this.occurredAt = time; this.action = action; this.actorId = actor; this.subjectId = subject;
    }
    public Long getId() { return id; }
    public Instant getOccurredAt() { return occurredAt; }
    public Action getAction() { return action; }
    public Long getActorId() { return actorId; }
    public Long getSubjectId() { return subjectId; }
}

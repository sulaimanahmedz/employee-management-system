package com.ems.identity.dto;
import java.time.Instant;
import com.ems.identity.entity.SecurityEvent;
public record SecurityEventView(Long id, Instant occurredAt, SecurityEvent.Action action, Long actorId, Long subjectId) {}

package com.ems.identity.dto;
import java.time.Instant;
/** Issued only to an authenticated ADMIN; never persisted or logged as plaintext. */
public record ResetTicket(String token, Instant expiresAt) {}

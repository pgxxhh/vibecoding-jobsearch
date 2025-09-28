package com.vibe.jobs.auth.domain;

import jakarta.persistence.*;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_login_challenge", indexes = {
        @Index(name = "idx_auth_login_challenge_email", columnList = "email")
})
public class LoginChallenge {
    private static final int MAX_ATTEMPTS = 5;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Embedded
    private EmailAddress email;

    @Column(name = "code_hash", nullable = false, length = 128)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_sent_at", nullable = false)
    private Instant lastSentAt;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @Column(name = "attempts", nullable = false)
    private int attemptCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LoginChallenge() {
        // for JPA
    }

    private LoginChallenge(UUID id, EmailAddress email, String codeHash, Instant expiresAt, Instant lastSentAt, Instant now) {
        this.id = id;
        this.email = email;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.lastSentAt = lastSentAt;
        this.createdAt = now;
        this.updatedAt = now;
        this.verified = false;
        this.attemptCount = 0;
    }

    public static LoginChallenge create(EmailAddress email, String codeHash, Instant now, Duration ttl) {
        Instant expiry = now.plus(ttl);
        return new LoginChallenge(UUID.randomUUID(), email, codeHash, expiry, now, now);
    }

    public void refreshCode(String newHash, Instant now, Duration ttl) {
        this.codeHash = newHash;
        this.expiresAt = now.plus(ttl);
        this.lastSentAt = now;
        this.updatedAt = now;
        this.verified = false;
        this.attemptCount = 0;
    }

    public boolean canResend(Instant now, Duration cooldown) {
        return lastSentAt.plus(cooldown).isBefore(now) || lastSentAt.plus(cooldown).equals(now);
    }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    public boolean verify(String incomingHash, Instant now) {
        if (verified) {
            return true;
        }
        if (attemptCount >= MAX_ATTEMPTS) {
            return false;
        }
        if (!codeHash.equals(incomingHash)) {
            attemptCount += 1;
            updatedAt = now;
            return false;
        }
        if (isExpired(now)) {
            return false;
        }
        verified = true;
        updatedAt = now;
        return true;
    }

    public UUID getId() {
        return id;
    }

    public EmailAddress getEmail() {
        return email;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getLastSentAt() {
        return lastSentAt;
    }

    public boolean isVerified() {
        return verified;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

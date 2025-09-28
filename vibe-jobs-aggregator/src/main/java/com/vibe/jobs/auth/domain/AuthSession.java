package com.vibe.jobs.auth.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_session", indexes = {
        @Index(name = "idx_auth_session_token", columnList = "token_hash", unique = true)
})
public class AuthSession {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "token_hash", nullable = false, length = 128)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected AuthSession() {
        // for JPA
    }

    private AuthSession(UUID id, UserAccount user, String tokenHash, Instant expiresAt, Instant now) {
        this.id = id;
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = now;
    }

    public static AuthSession create(UserAccount user, String tokenHash, Instant now, Instant expiresAt) {
        return new AuthSession(UUID.randomUUID(), user, tokenHash, expiresAt, now);
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && !expiresAt.isBefore(now);
    }

    public void revoke(Instant now) {
        this.revokedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}

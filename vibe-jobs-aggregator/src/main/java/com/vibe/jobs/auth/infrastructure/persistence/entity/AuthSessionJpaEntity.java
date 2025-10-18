package com.vibe.jobs.auth.infrastructure.persistence.entity;

import com.vibe.jobs.auth.domain.AuthSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_session", indexes = {
        @Index(name = "idx_auth_session_token", columnList = "token_hash", unique = true)
})
public class AuthSessionJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccountJpaEntity user;

    @Column(name = "token_hash", nullable = false, length = 128)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected AuthSessionJpaEntity() {
        // for JPA
    }

    private AuthSessionJpaEntity(UUID id,
                                 UserAccountJpaEntity user,
                                 String tokenHash,
                                 Instant expiresAt,
                                 Instant createdAt,
                                 Instant revokedAt) {
        this.id = id;
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.revokedAt = revokedAt;
    }

    public static AuthSessionJpaEntity fromDomain(AuthSession session, UserAccountJpaEntity user) {
        return new AuthSessionJpaEntity(
                session.getId(),
                user,
                session.getTokenHash(),
                session.getExpiresAt(),
                session.getCreatedAt(),
                session.getRevokedAt()
        );
    }

    public void updateFromDomain(AuthSession session, UserAccountJpaEntity user) {
        this.user = user;
        this.tokenHash = session.getTokenHash();
        this.expiresAt = session.getExpiresAt();
        this.createdAt = session.getCreatedAt();
        this.revokedAt = session.getRevokedAt();
    }

    public AuthSession toDomain() {
        return AuthSession.builder()
                .id(id)
                .user(user != null ? user.toDomain() : null)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .createdAt(createdAt)
                .revokedAt(revokedAt)
                .build();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UserAccountJpaEntity getUser() {
        return user;
    }

    public void setUser(UserAccountJpaEntity user) {
        this.user = user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
}

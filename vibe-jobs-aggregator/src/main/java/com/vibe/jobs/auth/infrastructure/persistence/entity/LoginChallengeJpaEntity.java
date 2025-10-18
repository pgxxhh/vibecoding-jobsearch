package com.vibe.jobs.auth.infrastructure.persistence.entity;

import com.vibe.jobs.auth.domain.EmailAddress;
import com.vibe.jobs.auth.domain.LoginChallenge;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_login_challenge", indexes = {
        @Index(name = "idx_auth_login_challenge_email", columnList = "email"),
        @Index(name = "idx_auth_login_challenge_deleted", columnList = "deleted")
})
@Where(clause = "deleted = false")
public class LoginChallengeJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

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

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    protected LoginChallengeJpaEntity() {
        // for JPA
    }

    private LoginChallengeJpaEntity(UUID id,
                                    String email,
                                    String codeHash,
                                    Instant expiresAt,
                                    Instant lastSentAt,
                                    boolean verified,
                                    int attemptCount,
                                    Instant createdAt,
                                    Instant updatedAt,
                                    boolean deleted) {
        this.id = id;
        this.email = email;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.lastSentAt = lastSentAt;
        this.verified = verified;
        this.attemptCount = attemptCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deleted = deleted;
    }

    public static LoginChallengeJpaEntity fromDomain(LoginChallenge challenge) {
        return new LoginChallengeJpaEntity(
                challenge.getId(),
                challenge.getEmail() != null ? challenge.getEmail().value() : null,
                challenge.getCodeHash(),
                challenge.getExpiresAt(),
                challenge.getLastSentAt(),
                challenge.isVerified(),
                challenge.getAttemptCount(),
                challenge.getCreatedAt(),
                challenge.getUpdatedAt(),
                !challenge.isNotDeleted()
        );
    }

    public void updateFromDomain(LoginChallenge challenge) {
        this.email = challenge.getEmail() != null ? challenge.getEmail().value() : null;
        this.codeHash = challenge.getCodeHash();
        this.expiresAt = challenge.getExpiresAt();
        this.lastSentAt = challenge.getLastSentAt();
        this.verified = challenge.isVerified();
        this.attemptCount = challenge.getAttemptCount();
        this.createdAt = challenge.getCreatedAt();
        this.updatedAt = challenge.getUpdatedAt();
        this.deleted = !challenge.isNotDeleted();
    }

    public LoginChallenge toDomain() {
        return LoginChallenge.builder()
                .id(id)
                .email(email != null ? EmailAddress.restored(email) : null)
                .codeHash(codeHash)
                .expiresAt(expiresAt)
                .lastSentAt(lastSentAt)
                .verified(verified)
                .attemptCount(attemptCount)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .deleted(deleted)
                .build();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public void setCodeHash(String codeHash) {
        this.codeHash = codeHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getLastSentAt() {
        return lastSentAt;
    }

    public void setLastSentAt(Instant lastSentAt) {
        this.lastSentAt = lastSentAt;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}

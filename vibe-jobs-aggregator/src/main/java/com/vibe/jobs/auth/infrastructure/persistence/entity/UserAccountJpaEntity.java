package com.vibe.jobs.auth.infrastructure.persistence.entity;

import com.vibe.jobs.auth.domain.EmailAddress;
import com.vibe.jobs.auth.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Index;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_user", indexes = {
        @Index(name = "idx_auth_user_email", columnList = "email", unique = true)
})
public class UserAccountJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected UserAccountJpaEntity() {
        // for JPA
    }

    private UserAccountJpaEntity(UUID id,
                                 String email,
                                 Instant createdAt,
                                 Instant updatedAt,
                                 Instant lastLoginAt) {
        this.id = id;
        this.email = email;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastLoginAt = lastLoginAt;
    }

    public static UserAccountJpaEntity fromDomain(UserAccount userAccount) {
        return new UserAccountJpaEntity(
                userAccount.getId(),
                userAccount.getEmail() != null ? userAccount.getEmail().value() : null,
                userAccount.getCreatedAt(),
                userAccount.getUpdatedAt(),
                userAccount.getLastLoginAt()
        );
    }

    public void updateFromDomain(UserAccount userAccount) {
        this.email = userAccount.getEmail() != null ? userAccount.getEmail().value() : null;
        this.createdAt = userAccount.getCreatedAt();
        this.updatedAt = userAccount.getUpdatedAt();
        this.lastLoginAt = userAccount.getLastLoginAt();
    }

    public UserAccount toDomain() {
        return UserAccount.builder()
                .id(id)
                .email(email != null ? EmailAddress.restored(email) : null)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .lastLoginAt(lastLoginAt)
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

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}

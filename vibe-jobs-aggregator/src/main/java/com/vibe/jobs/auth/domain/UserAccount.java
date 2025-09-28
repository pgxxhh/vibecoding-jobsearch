package com.vibe.jobs.auth.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_user", indexes = {
        @Index(name = "idx_auth_user_email", columnList = "email", unique = true)
})
public class UserAccount {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Embedded
    private EmailAddress email;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected UserAccount() {
        // for JPA
    }

    private UserAccount(UUID id, EmailAddress email, Instant now) {
        this.id = id;
        this.email = email;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static UserAccount create(EmailAddress email, Instant now) {
        return new UserAccount(UUID.randomUUID(), email, now);
    }

    public void markLogin(Instant now) {
        this.lastLoginAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public EmailAddress getEmail() {
        return email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }
}

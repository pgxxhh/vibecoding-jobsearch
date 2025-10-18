package com.vibe.jobs.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthSession {
    private UUID id;
    private UserAccount user;
    private String tokenHash;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant revokedAt;

    public static AuthSession create(UserAccount user, String tokenHash, Instant now, Instant expiresAt) {
        return AuthSession.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .createdAt(now)
                .build();
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && !expiresAt.isBefore(now);
    }

    public void revoke(Instant now) {
        this.revokedAt = now;
    }
}

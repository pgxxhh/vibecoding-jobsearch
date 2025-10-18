package com.vibe.jobs.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginChallenge {
    private static final int MAX_ATTEMPTS = 5;

    private UUID id;
    private EmailAddress email;
    private String codeHash;
    private Instant expiresAt;
    private Instant lastSentAt;
    private boolean verified;
    private int attemptCount;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean deleted;

    public static LoginChallenge create(EmailAddress email, String codeHash, Instant now, Duration ttl) {
        Instant expiry = now.plus(ttl);
        return LoginChallenge.builder()
                .id(UUID.randomUUID())
                .email(email)
                .codeHash(codeHash)
                .expiresAt(expiry)
                .lastSentAt(now)
                .createdAt(now)
                .updatedAt(now)
                .verified(false)
                .attemptCount(0)
                .deleted(false)
                .build();
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

    public void markDeleted(Instant now) {
        this.deleted = true;
        this.updatedAt = now;
    }

    public boolean isNotDeleted() {
        return !deleted;
    }
}

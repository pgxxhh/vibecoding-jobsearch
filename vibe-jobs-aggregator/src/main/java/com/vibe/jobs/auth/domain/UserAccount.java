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
public class UserAccount {
    private UUID id;
    private EmailAddress email;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastLoginAt;

    public static UserAccount create(EmailAddress email, Instant now) {
        return UserAccount.builder()
                .id(UUID.randomUUID())
                .email(email)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void markLogin(Instant now) {
        this.lastLoginAt = now;
        this.updatedAt = now;
    }
}

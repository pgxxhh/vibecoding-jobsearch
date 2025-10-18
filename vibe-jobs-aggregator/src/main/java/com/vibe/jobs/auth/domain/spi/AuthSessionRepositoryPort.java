package com.vibe.jobs.auth.domain.spi;

import com.vibe.jobs.auth.domain.AuthSession;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthSessionRepositoryPort {
    AuthSession save(AuthSession session);

    Optional<AuthSession> findByTokenHash(String tokenHash);

    List<AuthSession> findByExpiresAtBefore(Instant cutoff);

    void deleteAllByIds(List<UUID> ids);
}

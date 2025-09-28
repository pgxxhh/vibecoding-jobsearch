package com.vibe.jobs.auth.repo;

import com.vibe.jobs.auth.domain.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {
    Optional<AuthSession> findByTokenHash(String tokenHash);

    List<AuthSession> findByExpiresAtBefore(Instant cutoff);
}

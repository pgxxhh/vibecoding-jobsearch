package com.vibe.jobs.auth.repo;

import com.vibe.jobs.auth.domain.LoginChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface LoginChallengeRepository extends JpaRepository<LoginChallenge, UUID> {
    Optional<LoginChallenge> findTopByEmail_ValueOrderByCreatedAtDesc(String email);

    void deleteByExpiresAtBefore(Instant cutoff);
}

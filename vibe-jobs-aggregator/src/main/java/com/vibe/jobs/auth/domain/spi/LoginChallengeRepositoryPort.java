package com.vibe.jobs.auth.domain.spi;

import com.vibe.jobs.auth.domain.LoginChallenge;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface LoginChallengeRepositoryPort {
    LoginChallenge save(LoginChallenge challenge);

    Optional<LoginChallenge> findLatestByEmail(String email);

    Optional<LoginChallenge> findById(UUID id);

    void softDeleteExpiredBefore(Instant cutoff);

    void hardDeleteExpiredBefore(Instant cutoff);

    void softDeleteById(UUID id);
}

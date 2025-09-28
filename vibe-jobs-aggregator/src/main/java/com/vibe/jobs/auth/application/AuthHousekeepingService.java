package com.vibe.jobs.auth.application;

import com.vibe.jobs.auth.repo.AuthSessionRepository;
import com.vibe.jobs.auth.repo.LoginChallengeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class AuthHousekeepingService {
    private final LoginChallengeRepository challengeRepository;
    private final AuthSessionRepository sessionRepository;
    private final Clock clock;

    public AuthHousekeepingService(LoginChallengeRepository challengeRepository,
                                   AuthSessionRepository sessionRepository,
                                   Optional<Clock> clock) {
        this.challengeRepository = challengeRepository;
        this.sessionRepository = sessionRepository;
        this.clock = clock.orElse(Clock.systemUTC());
    }

    @Transactional
    public void cleanupExpired() {
        Instant now = Instant.now(clock);
        challengeRepository.deleteByExpiresAtBefore(now.minusSeconds(60));
        var expiredSessions = sessionRepository.findByExpiresAtBefore(now);
        if (!expiredSessions.isEmpty()) {
            sessionRepository.deleteAll(expiredSessions);
        }
    }
}

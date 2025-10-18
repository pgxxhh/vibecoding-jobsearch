package com.vibe.jobs.auth.application;

import com.vibe.jobs.auth.domain.AuthSession;
import com.vibe.jobs.auth.domain.spi.AuthSessionRepositoryPort;
import com.vibe.jobs.auth.domain.spi.LoginChallengeRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class AuthHousekeepingService {
    private final LoginChallengeRepositoryPort challengeRepository;
    private final AuthSessionRepositoryPort sessionRepository;
    private final Clock clock;

    public AuthHousekeepingService(LoginChallengeRepositoryPort challengeRepository,
                                   AuthSessionRepositoryPort sessionRepository,
                                   Optional<Clock> clock) {
        this.challengeRepository = challengeRepository;
        this.sessionRepository = sessionRepository;
        this.clock = clock.orElse(Clock.systemUTC());
    }

    @Transactional
    public void cleanupExpired() {
        Instant now = Instant.now(clock);
        // 使用软删除清理过期的登录挑战
        challengeRepository.softDeleteExpiredBefore(now.minusSeconds(60));

        // 对于会话，仍然使用物理删除，因为过期的会话数据敏感
        var expiredSessions = sessionRepository.findByExpiresAtBefore(now);
        if (!expiredSessions.isEmpty()) {
            sessionRepository.deleteAllByIds(expiredSessions.stream()
                    .map(AuthSession::getId)
                    .toList());
        }
    }

    /**
     * 物理删除过期数据（定期清理任务）
     */
    @Transactional
    public void hardCleanupExpired() {
        Instant cutoff = Instant.now(clock).minusSeconds(7 * 24 * 3600); // 7天前
        challengeRepository.hardDeleteExpiredBefore(cutoff);

        var expiredSessions = sessionRepository.findByExpiresAtBefore(cutoff);
        if (!expiredSessions.isEmpty()) {
            sessionRepository.deleteAllByIds(expiredSessions.stream()
                    .map(AuthSession::getId)
                    .toList());
        }
    }
}

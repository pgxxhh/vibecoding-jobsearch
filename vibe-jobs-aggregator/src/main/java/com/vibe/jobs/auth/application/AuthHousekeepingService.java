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
        // 使用软删除清理过期的登录挑战
        challengeRepository.softDeleteByExpiresAtBefore(now.minusSeconds(60));
        
        // 对于会话，仍然使用物理删除，因为过期的会话数据敏感
        var expiredSessions = sessionRepository.findByExpiresAtBefore(now);
        if (!expiredSessions.isEmpty()) {
            sessionRepository.deleteAll(expiredSessions);
        }
    }

    /**
     * 物理删除过期数据（定期清理任务）
     */
    @Transactional
    public void hardCleanupExpired() {
        Instant cutoff = Instant.now(clock).minusSeconds(7 * 24 * 3600); // 7天前
        challengeRepository.hardDeleteByExpiresAtBefore(cutoff);
        
        var expiredSessions = sessionRepository.findByExpiresAtBefore(cutoff);
        if (!expiredSessions.isEmpty()) {
            sessionRepository.deleteAll(expiredSessions);
        }
    }
}

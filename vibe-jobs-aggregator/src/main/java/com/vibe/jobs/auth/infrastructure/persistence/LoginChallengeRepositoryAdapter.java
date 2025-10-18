package com.vibe.jobs.auth.infrastructure.persistence;

import com.vibe.jobs.auth.domain.LoginChallenge;
import com.vibe.jobs.auth.domain.spi.LoginChallengeRepositoryPort;
import com.vibe.jobs.auth.infrastructure.persistence.entity.LoginChallengeJpaEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class LoginChallengeRepositoryAdapter implements LoginChallengeRepositoryPort {

    private final LoginChallengeJpaRepository loginChallengeJpaRepository;

    public LoginChallengeRepositoryAdapter(LoginChallengeJpaRepository loginChallengeJpaRepository) {
        this.loginChallengeJpaRepository = loginChallengeJpaRepository;
    }

    @Override
    @Transactional
    public LoginChallenge save(LoginChallenge challenge) {
        LoginChallengeJpaEntity entity;
        if (challenge.getId() != null) {
            entity = loginChallengeJpaRepository.findById(challenge.getId())
                    .orElse(LoginChallengeJpaEntity.fromDomain(challenge));
        } else {
            entity = LoginChallengeJpaEntity.fromDomain(challenge);
        }
        entity.updateFromDomain(challenge);
        LoginChallengeJpaEntity saved = loginChallengeJpaRepository.save(entity);
        LoginChallenge mapped = saved.toDomain();
        copyChallengeState(mapped, challenge);
        return mapped;
    }

    @Override
    public Optional<LoginChallenge> findLatestByEmail(String email) {
        return loginChallengeJpaRepository.findTopByEmailAndDeletedFalseOrderByCreatedAtDesc(email)
                .map(LoginChallengeJpaEntity::toDomain);
    }

    @Override
    public Optional<LoginChallenge> findById(UUID id) {
        return loginChallengeJpaRepository.findById(id).map(LoginChallengeJpaEntity::toDomain);
    }

    @Override
    @Transactional
    public void softDeleteExpiredBefore(Instant cutoff) {
        loginChallengeJpaRepository.softDeleteByExpiresAtBefore(cutoff);
    }

    @Override
    @Transactional
    public void hardDeleteExpiredBefore(Instant cutoff) {
        loginChallengeJpaRepository.hardDeleteByExpiresAtBefore(cutoff);
    }

    @Override
    @Transactional
    public void softDeleteById(UUID id) {
        loginChallengeJpaRepository.softDeleteById(id);
    }

    private void copyChallengeState(LoginChallenge source, LoginChallenge target) {
        if (target == null || source == null) {
            return;
        }
        target.setId(source.getId());
        target.setEmail(source.getEmail());
        target.setCodeHash(source.getCodeHash());
        target.setExpiresAt(source.getExpiresAt());
        target.setLastSentAt(source.getLastSentAt());
        target.setVerified(source.isVerified());
        target.setAttemptCount(source.getAttemptCount());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        target.setDeleted(!source.isNotDeleted());
    }
}

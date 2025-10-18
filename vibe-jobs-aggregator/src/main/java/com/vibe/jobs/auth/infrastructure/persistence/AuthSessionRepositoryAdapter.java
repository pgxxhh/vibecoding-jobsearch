package com.vibe.jobs.auth.infrastructure.persistence;

import com.vibe.jobs.auth.domain.AuthSession;
import com.vibe.jobs.auth.domain.spi.AuthSessionRepositoryPort;
import com.vibe.jobs.auth.infrastructure.persistence.entity.AuthSessionJpaEntity;
import com.vibe.jobs.auth.infrastructure.persistence.entity.UserAccountJpaEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class AuthSessionRepositoryAdapter implements AuthSessionRepositoryPort {

    private final AuthSessionJpaRepository authSessionJpaRepository;
    private final UserAccountJpaRepository userAccountJpaRepository;

    public AuthSessionRepositoryAdapter(AuthSessionJpaRepository authSessionJpaRepository,
                                        UserAccountJpaRepository userAccountJpaRepository) {
        this.authSessionJpaRepository = authSessionJpaRepository;
        this.userAccountJpaRepository = userAccountJpaRepository;
    }

    @Override
    @Transactional
    public AuthSession save(AuthSession session) {
        if (session.getUser() == null || session.getUser().getId() == null) {
            throw new IllegalArgumentException("Session must reference a persisted user account");
        }
        UserAccountJpaEntity userEntity = userAccountJpaRepository.findById(session.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("User account not found: " + session.getUser().getId()));

        AuthSessionJpaEntity entity;
        if (session.getId() != null) {
            entity = authSessionJpaRepository.findById(session.getId())
                    .orElse(AuthSessionJpaEntity.fromDomain(session, userEntity));
        } else {
            entity = AuthSessionJpaEntity.fromDomain(session, userEntity);
        }
        entity.updateFromDomain(session, userEntity);
        AuthSessionJpaEntity saved = authSessionJpaRepository.save(entity);
        AuthSession mapped = saved.toDomain();
        copySessionState(mapped, session);
        return mapped;
    }

    @Override
    public Optional<AuthSession> findByTokenHash(String tokenHash) {
        return authSessionJpaRepository.findByTokenHashWithUser(tokenHash)
                .map(AuthSessionJpaEntity::toDomain);
    }

    @Override
    public List<AuthSession> findByExpiresAtBefore(Instant cutoff) {
        List<AuthSessionJpaEntity> entities = authSessionJpaRepository.findByExpiresAtBeforeWithUser(cutoff);
        List<AuthSession> sessions = new ArrayList<>(entities.size());
        for (AuthSessionJpaEntity entity : entities) {
            sessions.add(entity.toDomain());
        }
        return sessions;
    }

    @Override
    @Transactional
    public void deleteAllByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        authSessionJpaRepository.deleteAllById(ids);
    }

    private void copySessionState(AuthSession source, AuthSession target) {
        if (source == null || target == null) {
            return;
        }
        target.setId(source.getId());
        target.setUser(source.getUser());
        target.setTokenHash(source.getTokenHash());
        target.setExpiresAt(source.getExpiresAt());
        target.setCreatedAt(source.getCreatedAt());
        target.setRevokedAt(source.getRevokedAt());
    }
}

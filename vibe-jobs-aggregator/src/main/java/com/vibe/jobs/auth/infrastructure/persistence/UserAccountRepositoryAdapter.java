package com.vibe.jobs.auth.infrastructure.persistence;

import com.vibe.jobs.auth.domain.UserAccount;
import com.vibe.jobs.auth.domain.spi.UserAccountRepositoryPort;
import com.vibe.jobs.auth.infrastructure.persistence.entity.UserAccountJpaEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class UserAccountRepositoryAdapter implements UserAccountRepositoryPort {

    private final UserAccountJpaRepository userAccountJpaRepository;

    public UserAccountRepositoryAdapter(UserAccountJpaRepository userAccountJpaRepository) {
        this.userAccountJpaRepository = userAccountJpaRepository;
    }

    @Override
    @Transactional
    public UserAccount save(UserAccount userAccount) {
        UserAccountJpaEntity entity;
        if (userAccount.getId() != null) {
            entity = userAccountJpaRepository.findById(userAccount.getId())
                    .orElse(UserAccountJpaEntity.fromDomain(userAccount));
        } else {
            entity = UserAccountJpaEntity.fromDomain(userAccount);
        }
        entity.updateFromDomain(userAccount);
        UserAccountJpaEntity saved = userAccountJpaRepository.save(entity);
        UserAccount mapped = saved.toDomain();
        copyUserState(mapped, userAccount);
        return mapped;
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return userAccountJpaRepository.findByEmail(email).map(UserAccountJpaEntity::toDomain);
    }

    @Override
    public Optional<UserAccount> findById(UUID id) {
        return userAccountJpaRepository.findById(id).map(UserAccountJpaEntity::toDomain);
    }

    private void copyUserState(UserAccount source, UserAccount target) {
        if (target == null || source == null) {
            return;
        }
        target.setId(source.getId());
        target.setEmail(source.getEmail());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        target.setLastLoginAt(source.getLastLoginAt());
    }
}

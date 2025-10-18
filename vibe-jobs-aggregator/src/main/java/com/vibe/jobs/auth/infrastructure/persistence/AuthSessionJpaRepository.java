package com.vibe.jobs.auth.infrastructure.persistence;

import com.vibe.jobs.auth.infrastructure.persistence.entity.AuthSessionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthSessionJpaRepository extends JpaRepository<AuthSessionJpaEntity, UUID> {

    @Query("SELECT s FROM AuthSessionJpaEntity s JOIN FETCH s.user WHERE s.tokenHash = :tokenHash")
    Optional<AuthSessionJpaEntity> findByTokenHashWithUser(@Param("tokenHash") String tokenHash);

    @Query("SELECT s FROM AuthSessionJpaEntity s JOIN FETCH s.user WHERE s.expiresAt < :cutoff")
    List<AuthSessionJpaEntity> findByExpiresAtBeforeWithUser(@Param("cutoff") Instant cutoff);
}

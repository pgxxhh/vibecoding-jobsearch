package com.vibe.jobs.auth.infrastructure.persistence;

import com.vibe.jobs.auth.infrastructure.persistence.entity.LoginChallengeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface LoginChallengeJpaRepository extends JpaRepository<LoginChallengeJpaEntity, UUID> {

    Optional<LoginChallengeJpaEntity> findTopByEmailAndDeletedFalseOrderByCreatedAtDesc(String email);

    @Modifying
    @Query("UPDATE LoginChallengeJpaEntity lc SET lc.deleted = true WHERE lc.expiresAt < :cutoff")
    void softDeleteByExpiresAtBefore(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("DELETE FROM LoginChallengeJpaEntity lc WHERE lc.expiresAt < :cutoff")
    void hardDeleteByExpiresAtBefore(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("UPDATE LoginChallengeJpaEntity lc SET lc.deleted = true WHERE lc.id = :id")
    void softDeleteById(@Param("id") UUID id);
}

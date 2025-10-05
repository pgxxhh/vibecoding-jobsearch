package com.vibe.jobs.auth.repo;

import com.vibe.jobs.auth.domain.LoginChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface LoginChallengeRepository extends JpaRepository<LoginChallenge, UUID> {
    
    @Query("SELECT lc FROM LoginChallenge lc WHERE lc.email.value = :email AND lc.deleted = false ORDER BY lc.createdAt DESC")
    Optional<LoginChallenge> findTopByEmail_ValueOrderByCreatedAtDesc(@Param("email") String email);

    @Modifying
    @Query("UPDATE LoginChallenge lc SET lc.deleted = true WHERE lc.expiresAt < :cutoff")
    void softDeleteByExpiresAtBefore(@Param("cutoff") Instant cutoff);

    // 保留原有的物理删除方法供清理过期数据使用
    @Modifying
    @Query("DELETE FROM LoginChallenge lc WHERE lc.expiresAt < :cutoff")
    void hardDeleteByExpiresAtBefore(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("UPDATE LoginChallenge lc SET lc.deleted = true WHERE lc.id = :id")
    void softDeleteById(@Param("id") UUID id);
}

package com.vibe.jobs.ingestion.infrastructure.jpa;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.Optional;

public interface SpringDataIngestionCursorRepository extends JpaRepository<IngestionCursorEntity, Long> {

    Optional<IngestionCursorEntity> findBySourceNameAndCompanyAndCategory(String sourceName, String company, String category);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    Optional<IngestionCursorEntity> findWithLockBySourceNameAndCompanyAndCategory(String sourceName, String company, String category);
}

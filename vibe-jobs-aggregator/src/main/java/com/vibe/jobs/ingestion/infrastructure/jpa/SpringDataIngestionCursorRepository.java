package com.vibe.jobs.ingestion.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataIngestionCursorRepository extends JpaRepository<IngestionCursorEntity, Long> {
    Optional<IngestionCursorEntity> findBySourceNameAndCompanyAndCategory(String sourceName, String company, String category);
}

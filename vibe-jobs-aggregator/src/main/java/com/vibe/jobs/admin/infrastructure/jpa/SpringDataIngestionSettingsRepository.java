package com.vibe.jobs.admin.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataIngestionSettingsRepository extends JpaRepository<IngestionSettingsEntity, Long> {
}

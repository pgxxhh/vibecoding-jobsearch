package com.vibe.jobs.admin.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataIngestionSettingsRepository extends JpaRepository<IngestionSettingsEntity, Long> {
    Optional<IngestionSettingsEntity> findBySettingKey(String settingKey);
}

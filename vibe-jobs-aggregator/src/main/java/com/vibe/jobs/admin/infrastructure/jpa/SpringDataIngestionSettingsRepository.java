package com.vibe.jobs.admin.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataIngestionSettingsRepository extends JpaRepository<IngestionSettingsEntity, Long> {
    Optional<IngestionSettingsEntity> findBySettingKey(String settingKey);
}

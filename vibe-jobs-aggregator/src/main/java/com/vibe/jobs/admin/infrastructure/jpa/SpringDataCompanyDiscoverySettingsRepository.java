package com.vibe.jobs.admin.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataCompanyDiscoverySettingsRepository extends JpaRepository<CompanyDiscoverySettingsEntity, Long> {

    Optional<CompanyDiscoverySettingsEntity> findBySettingKey(String settingKey);
}

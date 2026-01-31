package com.vibe.jobs.companydiscovery.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataCompanyDiscoveryResultRepository extends JpaRepository<CompanyDiscoveryResultEntity, Long> {

    List<CompanyDiscoveryResultEntity> findTop100ByDeletedFalseOrderByCreatedTimeDesc();
}

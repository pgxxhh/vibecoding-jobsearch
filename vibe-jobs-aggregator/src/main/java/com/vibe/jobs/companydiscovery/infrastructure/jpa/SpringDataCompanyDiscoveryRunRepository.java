package com.vibe.jobs.companydiscovery.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataCompanyDiscoveryRunRepository extends JpaRepository<CompanyDiscoveryRunEntity, Long> {

    List<CompanyDiscoveryRunEntity> findTop50ByDeletedFalseOrderByStartedAtDesc();
}

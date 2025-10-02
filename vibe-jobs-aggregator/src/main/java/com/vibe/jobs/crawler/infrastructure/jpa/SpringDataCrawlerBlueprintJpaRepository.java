package com.vibe.jobs.crawler.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataCrawlerBlueprintJpaRepository extends JpaRepository<CrawlerBlueprintEntity, String> {
    List<CrawlerBlueprintEntity> findAllByEnabledTrue();
}

package com.vibe.jobs.crawler.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataCrawlerBlueprintJpaRepository extends JpaRepository<CrawlerBlueprintEntity, String> {
    List<CrawlerBlueprintEntity> findAllByEnabledTrue();
}

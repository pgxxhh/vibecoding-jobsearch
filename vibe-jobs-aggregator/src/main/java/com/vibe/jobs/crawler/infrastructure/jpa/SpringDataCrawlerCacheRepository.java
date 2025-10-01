package com.vibe.jobs.crawler.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataCrawlerCacheRepository extends JpaRepository<CrawlerCacheEntryEntity, Long> {
    Optional<CrawlerCacheEntryEntity> findFirstByBlueprintCodeAndCacheKey(String blueprintCode, String cacheKey);
}

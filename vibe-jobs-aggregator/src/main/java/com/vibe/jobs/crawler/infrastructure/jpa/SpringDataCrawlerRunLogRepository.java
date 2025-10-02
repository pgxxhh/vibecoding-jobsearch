package com.vibe.jobs.crawler.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCrawlerRunLogRepository extends JpaRepository<CrawlerRunLogEntity, Long> {
}

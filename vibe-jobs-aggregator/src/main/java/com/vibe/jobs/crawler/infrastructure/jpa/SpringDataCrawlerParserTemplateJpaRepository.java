package com.vibe.jobs.crawler.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataCrawlerParserTemplateJpaRepository extends JpaRepository<CrawlerParserTemplateEntity, String> {
}

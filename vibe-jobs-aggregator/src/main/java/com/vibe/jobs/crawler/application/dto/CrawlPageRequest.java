package com.vibe.jobs.crawler.application.dto;

import com.vibe.jobs.crawler.domain.CrawlContext;
import com.vibe.jobs.crawler.domain.CrawlPagination;

public record CrawlPageRequest(String blueprintCode,
                               CrawlContext context,
                               CrawlPagination pagination) {
}

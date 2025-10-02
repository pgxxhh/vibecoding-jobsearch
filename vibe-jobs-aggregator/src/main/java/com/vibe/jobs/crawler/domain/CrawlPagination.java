package com.vibe.jobs.crawler.domain;

public record CrawlPagination(int page, int size) {

    public CrawlPagination {
        page = Math.max(1, page);
        size = Math.max(1, size);
    }

    public int offset() {
        return (page - 1) * size;
    }
}

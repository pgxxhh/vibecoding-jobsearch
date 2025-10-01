package com.vibe.jobs.crawler.domain;

public record CrawlPagination(int page, int size) {

    public CrawlPagination {
        int normalizedPage = Math.max(1, page);
        int normalizedSize = Math.max(1, size);
        this.page = normalizedPage;
        this.size = normalizedSize;
    }

    public int offset() {
        return (page - 1) * size;
    }
}

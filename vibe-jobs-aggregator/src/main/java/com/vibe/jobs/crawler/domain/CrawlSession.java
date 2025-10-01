package com.vibe.jobs.crawler.domain;

import java.time.Instant;
import java.util.UUID;

public class CrawlSession {

    private final String id;
    private final CrawlBlueprint blueprint;
    private final CrawlContext context;
    private final Instant startedAt;

    public CrawlSession(CrawlBlueprint blueprint, CrawlContext context) {
        this(UUID.randomUUID().toString(), blueprint, context, Instant.now());
    }

    public CrawlSession(String id, CrawlBlueprint blueprint, CrawlContext context, Instant startedAt) {
        this.id = id;
        this.blueprint = blueprint;
        this.context = context;
        this.startedAt = startedAt == null ? Instant.now() : startedAt;
    }

    public String id() {
        return id;
    }

    public CrawlBlueprint blueprint() {
        return blueprint;
    }

    public CrawlContext context() {
        return context;
    }

    public Instant startedAt() {
        return startedAt;
    }
}

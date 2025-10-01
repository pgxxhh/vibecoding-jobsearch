package com.vibe.jobs.crawler.domain;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregate root that describes how a specific career site should be crawled.
 */
public class CrawlBlueprint {

    private final String code;
    private final String name;
    private final boolean enabled;
    private final int concurrencyLimit;
    private final String entryUrl;
    private final PagingStrategy pagingStrategy;
    private final CrawlFlow flow;
    private final ParserProfile parserProfile;
    private final RateLimit rateLimit;
    private final Map<String, Object> metadata;

    public CrawlBlueprint(String code,
                          String name,
                          boolean enabled,
                          int concurrencyLimit,
                          String entryUrl,
                          PagingStrategy pagingStrategy,
                          CrawlFlow flow,
                          ParserProfile parserProfile,
                          RateLimit rateLimit,
                          Map<String, Object> metadata) {
        this.code = sanitize(code);
        this.name = sanitize(name);
        this.enabled = enabled;
        this.concurrencyLimit = concurrencyLimit <= 0 ? 1 : concurrencyLimit;
        this.entryUrl = sanitize(entryUrl);
        this.pagingStrategy = pagingStrategy == null ? PagingStrategy.disabled() : pagingStrategy;
        this.flow = flow == null ? CrawlFlow.empty() : flow;
        this.parserProfile = parserProfile == null ? ParserProfile.empty() : parserProfile;
        this.rateLimit = rateLimit == null ? RateLimit.unlimited() : rateLimit;
        this.metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(metadata);
    }

    private String sanitize(String value) {
        return value == null ? "" : value.trim();
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public boolean enabled() {
        return enabled;
    }

    public int concurrencyLimit() {
        return concurrencyLimit;
    }

    public String entryUrl() {
        return entryUrl;
    }

    public PagingStrategy pagingStrategy() {
        return pagingStrategy;
    }

    public CrawlFlow flow() {
        return flow;
    }

    public ParserProfile parserProfile() {
        return parserProfile;
    }

    public RateLimit rateLimit() {
        return rateLimit;
    }

    public Map<String, Object> metadata() {
        return metadata;
    }

    public boolean isConfigured() {
        return !entryUrl.isBlank() && parserProfile.isConfigured();
    }

    public String resolveEntryUrl(CrawlContext context, CrawlPagination pagination) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(pagination, "pagination");
        String base = context.entryUrlOverride().filter(url -> !url.isBlank()).orElse(entryUrl);
        if (base == null || base.isBlank()) {
            return "";
        }
        return pagingStrategy.apply(base, pagination);
    }

    public boolean allowsParallelism() {
        return concurrencyLimit > 1;
    }

    public Optional<Object> metadata(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(metadata.get(key));
    }

    public record RateLimit(int requestsPerMinute, int burst) {
        public static RateLimit of(int requestsPerMinute, int burst) {
            return new RateLimit(Math.max(0, requestsPerMinute), Math.max(1, burst));
        }

        public static RateLimit unlimited() {
            return new RateLimit(0, 1);
        }

        public boolean isLimited() {
            return requestsPerMinute > 0;
        }
    }
}

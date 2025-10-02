package com.vibe.jobs.crawler.infrastructure.jpa;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "crawler_blueprint")
public class CrawlerBlueprintEntity {

    @Id
    @Column(length = 128)
    private String code;

    @Column(nullable = false, length = 256)
    private String name;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "entry_url", length = 2048)
    private String entryUrl;

    @Column(name = "concurrency_limit")
    private int concurrencyLimit;

    @Column(name = "config_json", columnDefinition = "LONGTEXT")
    private String configJson;

    @Column(name = "parser_template_code", length = 128)
    private String parserTemplateCode;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getEntryUrl() {
        return entryUrl;
    }

    public int getConcurrencyLimit() {
        return concurrencyLimit;
    }

    public String getConfigJson() {
        return configJson;
    }

    public String getParserTemplateCode() {
        return parserTemplateCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

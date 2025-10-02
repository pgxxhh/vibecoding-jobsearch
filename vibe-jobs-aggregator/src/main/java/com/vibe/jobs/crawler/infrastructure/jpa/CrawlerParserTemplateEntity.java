package com.vibe.jobs.crawler.infrastructure.jpa;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "crawler_parser_template")
public class CrawlerParserTemplateEntity {

    @Id
    @Column(length = 128)
    private String code;

    @Column(length = 256)
    private String description;

    @Column(name = "config_json", columnDefinition = "LONGTEXT")
    private String configJson;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getConfigJson() {
        return configJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

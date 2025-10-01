package com.vibe.jobs.crawler.infrastructure.jpa;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "crawler_cache")
public class CrawlerCacheEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "blueprint_code", length = 128)
    private String blueprintCode;

    @Column(name = "cache_key", length = 256)
    private String cacheKey;

    @Lob
    @Column(name = "response_blob")
    private byte[] responseBlob;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public CrawlerCacheEntryEntity() {
    }

    public CrawlerCacheEntryEntity(String blueprintCode, String cacheKey, byte[] responseBlob, Instant expiresAt) {
        this.blueprintCode = blueprintCode;
        this.cacheKey = cacheKey;
        this.responseBlob = responseBlob;
        this.expiresAt = expiresAt;
    }
}

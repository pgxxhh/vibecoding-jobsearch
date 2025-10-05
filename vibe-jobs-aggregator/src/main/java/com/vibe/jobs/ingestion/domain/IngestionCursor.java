package com.vibe.jobs.ingestion.domain;

import java.time.Instant;
import java.util.Objects;

public record IngestionCursor(
        String sourceCode,
        String sourceName,
        String company,
        String category,
        Instant lastPostedAt,
        String lastExternalId,
        String nextPageToken,
        Instant lastIngestedAt,
        Instant createTime,
        Instant updateTime
) {
    public IngestionCursor {
        sourceCode = normalize(sourceCode);
        sourceName = normalize(sourceName);
        company = normalize(company);
        category = normalize(category);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    public boolean hasPosition() {
        return lastPostedAt != null || (lastExternalId != null && !lastExternalId.isBlank());
    }

    public IngestionCursor withNextPageToken(String token) {
        if (Objects.equals(token, nextPageToken)) {
            return this;
        }
        return new IngestionCursor(sourceCode, sourceName, company, category, lastPostedAt, lastExternalId, token, lastIngestedAt, createTime, updateTime);
    }

    public IngestionCursor advanceTo(Instant postedAt, String externalId) {
        if (postedAt == null && (externalId == null || externalId.isBlank())) {
            return this;
        }
        Instant now = Instant.now();
        Instant effectiveCreateTime = createTime == null ? now : createTime;
        return new IngestionCursor(sourceCode, sourceName, company, category, postedAt, externalId, nextPageToken, now, effectiveCreateTime, now);
    }
}

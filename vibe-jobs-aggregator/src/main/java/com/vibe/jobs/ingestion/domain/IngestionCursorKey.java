package com.vibe.jobs.ingestion.domain;

import java.util.Objects;

public record IngestionCursorKey(String sourceCode,
                                 String sourceName,
                                 String company,
                                 String category) {

    public IngestionCursorKey {
        sourceCode = normalize(sourceCode);
        sourceName = normalize(sourceName);
        company = normalize(company);
        category = normalize(category);
    }

    public static IngestionCursorKey of(String sourceCode, String sourceName, String company, String category) {
        return new IngestionCursorKey(sourceCode, sourceName, company, category);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    public boolean matches(IngestionCursor cursor) {
        if (cursor == null) {
            return false;
        }
        return Objects.equals(cursor.sourceCode(), sourceCode)
                && Objects.equals(cursor.sourceName(), sourceName)
                && Objects.equals(cursor.company(), company)
                && Objects.equals(cursor.category(), category);
    }
}

package com.vibe.jobs.admin.domain;

import java.util.Objects;

public record AdminChangeLogEntry(
        String actorEmail,
        String action,
        String resourceType,
        String resourceId,
        String diffJson
) {
    public AdminChangeLogEntry {
        Objects.requireNonNull(actorEmail, "actorEmail must not be null");
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(resourceType, "resourceType must not be null");
    }
}


package com.vibe.jobs.admin.domain.event;

import com.vibe.jobs.admin.domain.IngestionSettingsSnapshot;

public record IngestionSettingsUpdatedEvent(IngestionSettingsSnapshot snapshot) {
}

package com.vibe.jobs.admin.domain.event;

import com.vibe.jobs.admin.domain.CompanyDiscoverySettingsSnapshot;

public record CompanyDiscoverySettingsUpdatedEvent(CompanyDiscoverySettingsSnapshot snapshot) {
}

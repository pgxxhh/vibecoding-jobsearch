package com.vibe.jobs.web.subscription.dto;

import com.vibe.jobs.subscription.domain.JobAlertSubscriptionStatus;

import java.time.Instant;

public record SubscriptionResponse(Long id,
                                   String keyword,
                                   String company,
                                   String location,
                                   String level,
                                   String filters,
                                   Integer scheduleHour,
                                   String timezone,
                                   JobAlertSubscriptionStatus status,
                                   Instant lastNotifiedAt,
                                   Instant createdAt) {
}

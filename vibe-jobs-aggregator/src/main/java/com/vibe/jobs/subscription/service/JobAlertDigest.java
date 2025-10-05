package com.vibe.jobs.subscription.service;

import java.time.Instant;
import java.util.List;

public record JobAlertDigest(String subscriptionSummary,
                             String unsubscribeUrl,
                             List<JobItem> jobs) {

    public record JobItem(Long id,
                          String title,
                          String company,
                          String location,
                          Instant postedAt,
                          String url) {}
}

package com.vibe.jobs.web.subscription.dto;

import com.vibe.jobs.subscription.domain.JobAlertSubscriptionStatus;

import java.util.Map;

public record UpdateSubscriptionRequest(String keyword,
                                        String company,
                                        String location,
                                        String level,
                                        Map<String, Object> filters,
                                        Integer scheduleHour,
                                        String timezone,
                                        JobAlertSubscriptionStatus status) {
}

package com.vibe.jobs.subscription.service;

import com.vibe.jobs.subscription.domain.JobAlertSubscriptionStatus;

import java.util.Map;

public record UpdateJobAlertCommand(String keyword,
                                    String company,
                                    String location,
                                    String level,
                                    Map<String, Object> filters,
                                    Integer scheduleHour,
                                    String timezone,
                                    JobAlertSubscriptionStatus status) {
}

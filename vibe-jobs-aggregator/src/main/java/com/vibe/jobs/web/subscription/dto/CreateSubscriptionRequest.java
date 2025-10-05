package com.vibe.jobs.web.subscription.dto;

import java.util.Map;
public record CreateSubscriptionRequest(String keyword,
                                        String company,
                                        String location,
                                        String level,
                                        Map<String, Object> filters,
                                        Integer scheduleHour,
                                        String timezone) {
}

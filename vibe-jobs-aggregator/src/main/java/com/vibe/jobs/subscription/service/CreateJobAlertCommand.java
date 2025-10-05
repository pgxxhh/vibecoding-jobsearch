package com.vibe.jobs.subscription.service;

import java.util.Map;

public record CreateJobAlertCommand(String keyword,
                                    String company,
                                    String location,
                                    String level,
                                    Map<String, Object> filters,
                                    Integer scheduleHour,
                                    String timezone) {
}

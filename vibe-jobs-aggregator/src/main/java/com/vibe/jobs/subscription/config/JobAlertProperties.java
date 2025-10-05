package com.vibe.jobs.subscription.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "job-alerts")
public class JobAlertProperties {
    private int maxSubscriptionsPerUser = 10;
    private int defaultScheduleHour = 0;
    private String defaultTimezone = "Asia/Shanghai";
    private int digestMaxJobs = 20;
    private Duration schedulerLookback = Duration.ofHours(24);
    private String unsubscribeSecret = "change-me";
    private String unsubscribeBaseUrl = "https://jobs.vibecoding.com/alerts/unsubscribe";

    public int getMaxSubscriptionsPerUser() {
        return maxSubscriptionsPerUser;
    }

    public void setMaxSubscriptionsPerUser(int maxSubscriptionsPerUser) {
        this.maxSubscriptionsPerUser = maxSubscriptionsPerUser;
    }

    public int getDefaultScheduleHour() {
        return defaultScheduleHour;
    }

    public void setDefaultScheduleHour(int defaultScheduleHour) {
        this.defaultScheduleHour = defaultScheduleHour;
    }

    public String getDefaultTimezone() {
        return defaultTimezone;
    }

    public void setDefaultTimezone(String defaultTimezone) {
        this.defaultTimezone = defaultTimezone;
    }

    public int getDigestMaxJobs() {
        return digestMaxJobs;
    }

    public void setDigestMaxJobs(int digestMaxJobs) {
        this.digestMaxJobs = digestMaxJobs;
    }

    public Duration getSchedulerLookback() {
        return schedulerLookback;
    }

    public void setSchedulerLookback(Duration schedulerLookback) {
        this.schedulerLookback = schedulerLookback;
    }

    public String getUnsubscribeSecret() {
        return unsubscribeSecret;
    }

    public void setUnsubscribeSecret(String unsubscribeSecret) {
        this.unsubscribeSecret = unsubscribeSecret;
    }

    public String getUnsubscribeBaseUrl() {
        return unsubscribeBaseUrl;
    }

    public void setUnsubscribeBaseUrl(String unsubscribeBaseUrl) {
        this.unsubscribeBaseUrl = unsubscribeBaseUrl;
    }
}

package com.vibe.jobs.subscription.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_alert_subscription", indexes = {
        @Index(name = "idx_job_alert_user_status", columnList = "user_id, status"),
        @Index(name = "idx_job_alert_schedule", columnList = "status, schedule_hour")
})
public class JobAlertSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(nullable = false)
    private String email;

    @Column(name = "search_keyword")
    private String searchKeyword;

    private String company;

    private String location;

    private String level;

    @Lob
    @Column(name = "filters_json")
    private String filtersJson;

    @Column(name = "schedule_hour", nullable = false)
    private int scheduleHour;

    @Column(nullable = false)
    private String timezone;

    @Column(name = "last_notified_at", columnDefinition = "timestamp")
    private Instant lastNotifiedAt;

    @Column(name = "last_seen_cursor", columnDefinition = "varbinary(64)")
    private String lastSeenCursor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobAlertSubscriptionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamp")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSearchKeyword() {
        return searchKeyword;
    }

    public void setSearchKeyword(String searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getFiltersJson() {
        return filtersJson;
    }

    public void setFiltersJson(String filtersJson) {
        this.filtersJson = filtersJson;
    }

    public int getScheduleHour() {
        return scheduleHour;
    }

    public void setScheduleHour(int scheduleHour) {
        this.scheduleHour = scheduleHour;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Instant getLastNotifiedAt() {
        return lastNotifiedAt;
    }

    public void setLastNotifiedAt(Instant lastNotifiedAt) {
        this.lastNotifiedAt = lastNotifiedAt;
    }

    public String getLastSeenCursor() {
        return lastSeenCursor;
    }

    public void setLastSeenCursor(String lastSeenCursor) {
        this.lastSeenCursor = lastSeenCursor;
    }

    public JobAlertSubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(JobAlertSubscriptionStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

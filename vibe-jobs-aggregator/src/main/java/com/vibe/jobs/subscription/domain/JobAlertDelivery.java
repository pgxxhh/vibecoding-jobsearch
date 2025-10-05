package com.vibe.jobs.subscription.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "job_alert_delivery", indexes = {
        @Index(name = "idx_job_alert_delivery_subscription", columnList = "subscription_id, delivered_at DESC")
})
public class JobAlertDelivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private JobAlertSubscription subscription;

    @Column(name = "delivered_at", nullable = false, columnDefinition = "timestamp")
    private Instant deliveredAt;

    @Column(name = "job_count", nullable = false)
    private int jobCount;

    @Lob
    @Column(name = "job_ids")
    private String jobIds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobAlertDeliveryStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp")
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public JobAlertSubscription getSubscription() {
        return subscription;
    }

    public void setSubscription(JobAlertSubscription subscription) {
        this.subscription = subscription;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public int getJobCount() {
        return jobCount;
    }

    public void setJobCount(int jobCount) {
        this.jobCount = jobCount;
    }

    public String getJobIds() {
        return jobIds;
    }

    public void setJobIds(String jobIds) {
        this.jobIds = jobIds;
    }

    public JobAlertDeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(JobAlertDeliveryStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

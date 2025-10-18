package com.vibe.jobs.jobposting.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    private Long id;
    private String source;
    private String externalId;
    private String title;
    private String company;
    private String location;
    private String level;
    private Instant postedAt;

    @Builder.Default
    private Set<String> tags = new HashSet<>();

    private String url;
    private Instant createdAt;
    private Instant updatedAt;
    private String checksum;

    @Builder.Default
    private boolean deleted = false;

    public void markCreated(Instant timestamp) {
        Instant safeTimestamp = Objects.requireNonNullElseGet(timestamp, Instant::now);
        this.createdAt = safeTimestamp;
        this.updatedAt = safeTimestamp;
    }

    public void markUpdated(Instant timestamp) {
        this.updatedAt = Objects.requireNonNullElseGet(timestamp, Instant::now);
    }

    public void delete() {
        delete(null);
    }

    public void delete(Instant deletedAt) {
        this.deleted = true;
        this.updatedAt = Objects.requireNonNullElseGet(deletedAt, Instant::now);
    }

    public boolean isNotDeleted() {
        return !deleted;
    }
}

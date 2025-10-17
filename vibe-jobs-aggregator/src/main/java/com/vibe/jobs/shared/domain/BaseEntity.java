package com.vibe.jobs.shared.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {
    
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;
    
    @Column(name = "created_time", nullable = false, updatable = false)
    private Instant createdTime;
    
    @Column(name = "updated_time", nullable = false)
    private Instant updatedTime;
    
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdTime == null) {
            createdTime = now;
        }
        updatedTime = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedTime = Instant.now();
    }
    
    public void delete() {
        this.deleted = true;
        this.updatedTime = Instant.now();
    }
    
    public boolean isNotDeleted() {
        return !deleted;
    }
}
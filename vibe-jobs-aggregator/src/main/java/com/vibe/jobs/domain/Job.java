package com.vibe.jobs.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "jobs", indexes = {
        @Index(name = "idx_jobs_source_extid", columnList = "source, externalId, deleted"),
        @Index(name = "idx_jobs_title", columnList = "title"),
        @Index(name = "idx_jobs_company", columnList = "company"),
        @Index(name = "idx_jobs_location", columnList = "location"),
        @Index(name = "idx_jobs_deleted", columnList = "deleted")
})
@Where(clause = "deleted = false")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Job {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false) 
    private String source;
    
    @Column(nullable = false) 
    private String externalId;
    
    @Column(nullable = false) 
    private String title;
    
    @Column(nullable = false) 
    private String company;
    
    private String location;
    private String level;
    
    @Column(columnDefinition = "timestamp") 
    private Instant postedAt;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_tags", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "tag")
    @Builder.Default
    private Set<String> tags = new HashSet<>();
    
    @Column(length = 1024) 
    private String url;
    
    @Column(nullable = false, updatable = false, columnDefinition = "timestamp")
    private Instant createdAt;
    
    @Column(nullable = false, columnDefinition = "timestamp")
    private Instant updatedAt;
    
    @Column(length = 64) 
    private String checksum;
    
    // 软删除字段
    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;
    
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
    
    public void delete() {
        this.deleted = true;
        this.updatedAt = Instant.now();
    }
    
    public boolean isNotDeleted() {
        return !deleted;
    }
}

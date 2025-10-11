package com.vibe.jobs.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Entity
@Table(name = "job_details", indexes = {
        @Index(name = "idx_job_details_job_id", columnList = "job_id", unique = true),
        @Index(name = "idx_job_details_deleted", columnList = "deleted")
})
@Where(clause = "deleted = false")
public class JobDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private Job job;

    @Lob
    @Column(columnDefinition = "longtext")
    private String content;

    @Lob
    @Column(name = "content_text", columnDefinition = "longtext")
    private String contentText;

    @Lob
    @Column(name = "summary", columnDefinition = "longtext")
    private String summary;

    @Lob
    @Column(name = "structured_data", columnDefinition = "longtext")
    private String structuredData;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "job_detail_skills", joinColumns = @JoinColumn(name = "job_detail_id"))
    @SQLRestriction("deleted = 0")
    @Column(name = "skill")
    @OrderColumn(name = "list_order")
    private List<String> skills = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "job_detail_highlights", joinColumns = @JoinColumn(name = "job_detail_id"))
    @SQLRestriction("deleted = 0")
    @Column(name = "highlight")
    @OrderColumn(name = "list_order")
    private List<String> highlights = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "timestamp")
    private Instant createdAt;

    @Column(nullable = false, columnDefinition = "timestamp")
    private Instant updatedAt;

    // 软删除字段
    @Column(nullable = false)
    private boolean deleted = false;

    protected JobDetail() {
    }

    public JobDetail(Job job, String content, String contentText) {
        this.job = job;
        this.content = content;
        this.contentText = contentText;
    }

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

    public Long getId() {
        return id;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getStructuredData() {
        return structuredData;
    }

    public void setStructuredData(String structuredData) {
        this.structuredData = structuredData;
    }

    public List<String> getSkills() {
        return skills;
    }

    public boolean replaceSkills(List<String> newSkills) {
        return replaceList(this.skills, newSkills);
    }

    public List<String> getHighlights() {
        return highlights;
    }

    public boolean replaceHighlights(List<String> newHighlights) {
        return replaceList(this.highlights, newHighlights);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    private boolean replaceList(List<String> target, List<String> incoming) {
        List<String> normalized = normalizeList(incoming);
        if (listsEqual(target, normalized)) {
            return false;
        }
        target.clear();
        target.addAll(normalized);
        return true;
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        ArrayList::new));
    }

    private boolean listsEqual(List<String> current, List<String> other) {
        if (current == other) {
            return true;
        }
        if (current == null || other == null) {
            return false;
        }
        if (current.size() != other.size()) {
            return false;
        }
        for (int i = 0; i < current.size(); i++) {
            if (!Objects.equals(current.get(i), other.get(i))) {
                return false;
            }
        }
        return true;
    }
}

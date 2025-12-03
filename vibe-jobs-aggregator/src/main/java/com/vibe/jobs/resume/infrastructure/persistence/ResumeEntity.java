package com.vibe.jobs.resume.infrastructure.persistence;

import com.vibe.jobs.resume.domain.Resume;
import com.vibe.jobs.resume.domain.ResumeParseStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.Where;

import java.time.Instant;

@Entity
@Table(name = "resume")
@Where(clause = "deleted = false")
public class ResumeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "language")
    private String language;

    @Column(name = "parsed_json", columnDefinition = "longtext")
    private String parsedJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "parse_status", nullable = false)
    private ResumeParseStatus parseStatus = ResumeParseStatus.PENDING;

    @Column(name = "create_time", nullable = false, updatable = false)
    private Instant createTime;

    @Column(name = "update_time", nullable = false)
    private Instant updateTime;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createTime = now;
        this.updateTime = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updateTime = Instant.now();
    }

    public Resume toDomain() {
        return Resume.builder()
                .id(id)
                .userId(userId)
                .originalFilename(originalFilename)
                .filePath(filePath)
                .language(language)
                .parsedJson(parsedJson)
                .parseStatus(parseStatus)
                .createTime(createTime)
                .updateTime(updateTime)
                .deleted(deleted)
                .build();
    }

    public static ResumeEntity fromDomain(Resume resume) {
        ResumeEntity entity = new ResumeEntity();
        entity.id = resume.getId();
        entity.userId = resume.getUserId();
        entity.originalFilename = resume.getOriginalFilename();
        entity.filePath = resume.getFilePath();
        entity.language = resume.getLanguage();
        entity.parsedJson = resume.getParsedJson();
        entity.parseStatus = resume.getParseStatus() == null ? ResumeParseStatus.PENDING : resume.getParseStatus();
        entity.deleted = resume.isDeleted();
        entity.createTime = resume.getCreateTime();
        entity.updateTime = resume.getUpdateTime();
        return entity;
    }

    public void updateFromDomain(Resume resume) {
        this.userId = resume.getUserId();
        this.originalFilename = resume.getOriginalFilename();
        this.filePath = resume.getFilePath();
        this.language = resume.getLanguage();
        this.parsedJson = resume.getParsedJson();
        this.parseStatus = resume.getParseStatus() == null ? ResumeParseStatus.PENDING : resume.getParseStatus();
        this.deleted = resume.isDeleted();
        this.createTime = resume.getCreateTime();
        this.updateTime = resume.getUpdateTime();
    }
}

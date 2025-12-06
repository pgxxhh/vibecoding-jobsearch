package com.vibe.jobs.resume.infrastructure.persistence;

import com.vibe.jobs.resume.domain.ResumeFeedback;
import com.vibe.jobs.resume.domain.ResumeFeedbackType;
import jakarta.persistence.*;
import org.hibernate.annotations.Where;

import java.time.Instant;

@Entity
@Table(name = "resume_feedback")
@Where(clause = "deleted = false")
public class ResumeFeedbackEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resume_id", nullable = false)
    private Long resumeId;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback", nullable = false)
    private ResumeFeedbackType feedback;

    @Column(name = "comment", columnDefinition = "text")
    private String comment;

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

    public ResumeFeedback toDomain() {
        return ResumeFeedback.builder()
                .id(id)
                .resumeId(resumeId)
                .jobId(jobId)
                .feedback(feedback)
                .comment(comment)
                .createTime(createTime)
                .updateTime(updateTime)
                .deleted(deleted)
                .build();
    }

    public static ResumeFeedbackEntity fromDomain(ResumeFeedback feedback) {
        ResumeFeedbackEntity entity = new ResumeFeedbackEntity();
        entity.id = feedback.getId();
        entity.resumeId = feedback.getResumeId();
        entity.jobId = feedback.getJobId();
        entity.feedback = feedback.getFeedback();
        entity.comment = feedback.getComment();
        entity.createTime = feedback.getCreateTime();
        entity.updateTime = feedback.getUpdateTime();
        entity.deleted = feedback.isDeleted();
        return entity;
    }
}

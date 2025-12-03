package com.vibe.jobs.resume.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeFeedback {
    private Long id;
    private Long resumeId;
    private Long jobId;
    private ResumeFeedbackType feedback;
    private String comment;
    private Instant createTime;
    private Instant updateTime;
    private boolean deleted;
}

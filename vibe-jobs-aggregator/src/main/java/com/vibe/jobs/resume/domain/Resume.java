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
public class Resume {
    private Long id;
    private Long userId;
    private String originalFilename;
    private String filePath;
    private String language;
    private String parsedJson;
    private ResumeParseStatus parseStatus;
    private Instant createTime;
    private Instant updateTime;
    private boolean deleted;
}

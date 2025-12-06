package com.vibe.jobs.resume.interfaces.rest.dto;

import com.vibe.jobs.resume.domain.ResumeParseStatus;

public record ResumeUploadResponse(Long resumeId, ResumeParseStatus parseStatus) {
}

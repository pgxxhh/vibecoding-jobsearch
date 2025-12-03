package com.vibe.jobs.resume.interfaces.rest.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ResumeFeedbackRequest(@NotEmpty List<ResumeFeedbackItem> items) {
    public record ResumeFeedbackItem(@NotNull Long jobId,
                                     @NotNull String feedback,
                                     String comment) {
    }
}

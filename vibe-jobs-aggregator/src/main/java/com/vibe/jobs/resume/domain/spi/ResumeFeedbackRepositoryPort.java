package com.vibe.jobs.resume.domain.spi;

import com.vibe.jobs.resume.domain.ResumeFeedback;

import java.util.List;

public interface ResumeFeedbackRepositoryPort {
    List<ResumeFeedback> saveAll(List<ResumeFeedback> feedback);
}

package com.vibe.jobs.resume.application;

import com.vibe.jobs.resume.domain.Resume;
import com.vibe.jobs.resume.domain.ResumeFeedback;
import com.vibe.jobs.resume.domain.spi.ResumeFeedbackRepositoryPort;
import com.vibe.jobs.resume.domain.spi.ResumeRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ResumeFeedbackService {
    private static final Logger log = LoggerFactory.getLogger(ResumeFeedbackService.class);
    private final ResumeRepositoryPort resumeRepositoryPort;
    private final ResumeFeedbackRepositoryPort resumeFeedbackRepositoryPort;

    public ResumeFeedbackService(ResumeRepositoryPort resumeRepositoryPort,
                                 ResumeFeedbackRepositoryPort resumeFeedbackRepositoryPort) {
        this.resumeRepositoryPort = resumeRepositoryPort;
        this.resumeFeedbackRepositoryPort = resumeFeedbackRepositoryPort;
    }

    public List<ResumeFeedback> saveFeedback(Long resumeId, List<ResumeFeedback> feedbackItems) {
        Resume resume = resumeRepositoryPort.findById(resumeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found"));
        if (resume.isDeleted()) {
            log.warn("Attempted to save feedback for deleted resume {}", resumeId);
            throw new ResponseStatusException(HttpStatus.GONE, "Resume deleted");
        }
        for (ResumeFeedback feedback : feedbackItems) {
            feedback.setResumeId(resumeId);
        }
        List<ResumeFeedback> saved = resumeFeedbackRepositoryPort.saveAll(feedbackItems);
        log.info("Saved {} resume feedback entries for resume {}", saved.size(), resumeId);
        return saved;
    }
}

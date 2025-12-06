package com.vibe.jobs.resume.interfaces.rest;

import com.vibe.jobs.resume.application.ResumeFeedbackService;
import com.vibe.jobs.resume.application.ResumeRecommendationRequest;
import com.vibe.jobs.resume.application.ResumeRecommendationService;
import com.vibe.jobs.resume.application.ResumeService;
import com.vibe.jobs.resume.domain.ResumeFeedback;
import com.vibe.jobs.resume.domain.ResumeFeedbackType;
import com.vibe.jobs.resume.interfaces.rest.dto.RecommendationItemResponse;
import com.vibe.jobs.resume.interfaces.rest.dto.RecommendationResponse;
import com.vibe.jobs.resume.interfaces.rest.dto.ResumeFeedbackRequest;
import com.vibe.jobs.resume.interfaces.rest.dto.ResumeFeedbackResponse;
import com.vibe.jobs.resume.interfaces.rest.dto.ResumeUploadResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/resumes")
@Validated
public class ResumeController {

    private static final Logger log = LoggerFactory.getLogger(ResumeController.class);
    private final ResumeService resumeService;
    private final ResumeRecommendationService resumeRecommendationService;
    private final ResumeFeedbackService resumeFeedbackService;

    public ResumeController(ResumeService resumeService,
                            ResumeRecommendationService resumeRecommendationService,
                            ResumeFeedbackService resumeFeedbackService) {
        this.resumeService = resumeService;
        this.resumeRecommendationService = resumeRecommendationService;
        this.resumeFeedbackService = resumeFeedbackService;
    }

    @PostMapping("/upload")
    public ResumeUploadResponse upload(@RequestPart("file") MultipartFile file,
                                       @RequestParam(value = "userId", required = false) Long userId) throws IOException {
        try {
            var resume = resumeService.upload(file, userId);
            log.info("Uploaded resume id={} for userId={}", resume.getId(), userId);
            return new ResumeUploadResponse(resume.getId(), resume.getParseStatus());
        } catch (IllegalArgumentException ex) {
            log.warn("Rejected resume upload for userId={} reason={} ", userId, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/{id}/recommendations")
    public RecommendationResponse recommend(@PathVariable("id") Long resumeId,
                                            @RequestParam(value = "location", required = false) String location,
                                            @RequestParam(value = "limit", required = false) @Min(1) @Max(30) Integer limit,
                                            @RequestParam(value = "experienceYears", required = false) Integer experienceYears,
                                            @RequestParam(value = "minSalary", required = false) Integer minSalary,
                                            @RequestParam(value = "remote", required = false) Boolean remote) {
        var request = ResumeRecommendationRequest.builder()
                .resumeId(resumeId)
                .location(location)
                .limit(limit)
                .experienceYears(experienceYears)
                .minSalary(minSalary)
                .remoteOnly(remote)
                .build();
        var recommendations = resumeRecommendationService.recommend(request);
        log.info("Returned {} recommendations for resume {}", recommendations.size(), resumeId);
        List<RecommendationItemResponse> items = recommendations.stream()
                .map(rec -> new RecommendationItemResponse(rec.getJobId(), rec.getTitle(), rec.getCompany(), rec.getLocation(),
                        rec.getScore(), rec.getSkillHits(), rec.getExplanation()))
                .toList();
        return new RecommendationResponse(items);
    }

    @PostMapping("/{id}/feedback")
    public ResumeFeedbackResponse feedback(@PathVariable("id") Long resumeId,
                                           @Valid @RequestBody ResumeFeedbackRequest request) {
        List<ResumeFeedback> items = request.items().stream()
                .map(item -> ResumeFeedback.builder()
                        .resumeId(resumeId)
                        .jobId(item.jobId())
                        .feedback(parseFeedback(item.feedback()))
                        .comment(item.comment())
                        .build())
                .collect(Collectors.toList());
        var saved = resumeFeedbackService.saveFeedback(resumeId, items);
        log.info("Accepted {} feedback items for resume {}", saved.size(), resumeId);
        return new ResumeFeedbackResponse(saved.size());
    }

    private ResumeFeedbackType parseFeedback(String feedback) {
        try {
            return ResumeFeedbackType.valueOf(feedback.toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported feedback type");
        }
    }
}

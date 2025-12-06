package com.vibe.jobs.resume.application;

import com.vibe.jobs.jobposting.application.JobDetailService;
import com.vibe.jobs.jobposting.domain.Job;
import com.vibe.jobs.jobposting.domain.JobDetail;
import com.vibe.jobs.jobposting.domain.spi.JobRepositoryPort;
import com.vibe.jobs.resume.domain.Resume;
import com.vibe.jobs.resume.domain.ResumeParseStatus;
import com.vibe.jobs.resume.domain.ResumeProfile;
import com.vibe.jobs.resume.domain.ResumeRecommendation;
import com.vibe.jobs.resume.domain.spi.ResumeRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ResumeRecommendationService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 30;
    private static final Logger log = LoggerFactory.getLogger(ResumeRecommendationService.class);
    private final ResumeRepositoryPort resumeRepositoryPort;
    private final JobRepositoryPort jobRepositoryPort;
    private final JobDetailService jobDetailService;
    private final ResumeService resumeService;
    private final RagExplanationService ragExplanationService;

    public ResumeRecommendationService(ResumeRepositoryPort resumeRepositoryPort,
                                       JobRepositoryPort jobRepositoryPort,
                                       JobDetailService jobDetailService,
                                       ResumeService resumeService,
                                       RagExplanationService ragExplanationService) {
        this.resumeRepositoryPort = resumeRepositoryPort;
        this.jobRepositoryPort = jobRepositoryPort;
        this.jobDetailService = jobDetailService;
        this.resumeService = resumeService;
        this.ragExplanationService = ragExplanationService;
    }

    public List<ResumeRecommendation> recommend(ResumeRecommendationRequest request) {
        Resume resume = resumeRepositoryPort.findById(request.getResumeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found"));
        if (resume.isDeleted()) {
            log.warn("Attempted recommendation for deleted resume {}", request.getResumeId());
            throw new ResponseStatusException(HttpStatus.GONE, "Resume deleted");
        }
        if (resume.getParseStatus() == ResumeParseStatus.FAILED) {
            log.warn("Attempted recommendation for failed resume parsing: {}", request.getResumeId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resume parsing failed");
        }
        ResumeProfile profile = resumeService.readProfile(resume);
        String query = buildQuery(profile);
        int limit = normalizeLimit(request.getLimit());
        log.info("Generating recommendations for resume={} location={} limit={}", request.getResumeId(), request.getLocation(), limit);
        List<Job> jobs = jobRepositoryPort.searchAfter(query, null, normalize(request.getLocation()), null,
                null, null, null, true, 0, limit);

        List<ResumeRecommendation> recommendations = new ArrayList<>();
        for (Job job : jobs) {
            JobDetail jobDetail = jobDetailService.findByJob(job).orElse(null);
            List<String> skillHits = intersectSkills(profile, job);
            double score = computeScore(skillHits, job.getPostedAt());
            String explanation = ragExplanationService.buildExplanation(profile, job, jobDetail, skillHits);
            recommendations.add(ResumeRecommendation.builder()
                    .jobId(job.getId())
                    .title(job.getTitle())
                    .company(job.getCompany())
                    .location(job.getLocation())
                    .score(score)
                    .skillHits(skillHits)
                    .explanation(explanation)
                    .build());
        }
        recommendations.sort(Comparator.comparingDouble(ResumeRecommendation::getScore).reversed());
        log.info("Generated {} recommendations for resume {}", recommendations.size(), request.getResumeId());
        return recommendations;
    }

    private String buildQuery(ResumeProfile profile) {
        Set<String> keywords = new HashSet<>(profile.getSkills());
        if (keywords.isEmpty() && profile.getRawText() != null) {
            keywords.addAll(List.of(profile.getRawText().split("\\s+")));
        }
        return keywords.stream()
                .filter(s -> s != null && s.length() > 2)
                .limit(8)
                .collect(Collectors.joining(" "));
    }

    private List<String> intersectSkills(ResumeProfile profile, Job job) {
        Set<String> jobTokens = new HashSet<>();
        if (job.getTitle() != null) {
            jobTokens.addAll(tokenize(job.getTitle()));
        }
        if (job.getCompany() != null) {
            jobTokens.addAll(tokenize(job.getCompany()));
        }
        if (job.getTags() != null) {
            jobTokens.addAll(job.getTags().stream()
                    .filter(Objects::nonNull)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet()));
        }
        return profile.getSkills().stream()
                .map(String::toLowerCase)
                .filter(jobTokens::contains)
                .limit(6)
                .toList();
    }

    private double computeScore(List<String> skillHits, Instant postedAt) {
        double base = skillHits.size();
        if (postedAt != null) {
            long ageDays = Math.max(1L, (Instant.now().getEpochSecond() - postedAt.getEpochSecond()) / 86400L);
            base += 1.0 / ageDays;
        }
        return base;
    }

    private int normalizeLimit(Integer requested) {
        if (requested == null || requested <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(requested, MAX_LIMIT);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private Set<String> tokenize(String value) {
        if (value == null) {
            return Set.of();
        }
        String[] parts = value.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        Set<String> tokens = new HashSet<>();
        for (String part : parts) {
            if (part.length() > 2) {
                tokens.add(part);
            }
        }
        return tokens;
    }
}

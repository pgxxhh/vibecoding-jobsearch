package com.vibe.jobs.service;

import com.vibe.jobs.domain.Job;
import com.vibe.jobs.domain.JobDetail;
import com.vibe.jobs.repo.JobDetailRepository;
import com.vibe.jobs.service.enrichment.JobContentEnrichment;
import com.vibe.jobs.service.enrichment.JobContentEnrichmentClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JobDetailService {

    private final JobDetailRepository repository;
    private final JobContentEnrichmentClient enrichmentClient;

    public JobDetailService(JobDetailRepository repository,
                            JobContentEnrichmentClient enrichmentClient) {
        this.repository = repository;
        this.enrichmentClient = enrichmentClient;
    }

    @Transactional
    public void saveContent(Job job, String content) {
        if (job == null) {
            return;
        }
        if (content == null || content.isBlank()) {
            return;
        }

        Long jobId = job.getId();
        if (jobId == null) {
            return;
        }

        String contentText = HtmlTextExtractor.toPlainText(content);
        JobDetail detail = repository.findByJobId(jobId)
                .orElseGet(() -> new JobDetail(job, content, contentText));

        boolean changed = detail.getId() == null;
        boolean contentChanged = false;
        if (!content.equals(detail.getContent())) {
            detail.setContent(content);
            changed = true;
            contentChanged = true;
        }

        if (!Objects.equals(contentText, detail.getContentText())) {
            detail.setContentText(contentText);
            changed = true;
            contentChanged = true;
        }

        if (contentChanged) {
            Optional<JobContentEnrichment> enrichment = enrichmentClient.enrich(job, content, contentText);
            if (enrichment.isPresent() && applyEnrichment(detail, enrichment.get())) {
                changed = true;
            }
        }

        if (changed) {
            repository.save(detail);
        }
    }

    @Transactional(readOnly = true)
    public Optional<JobDetail> findByJob(Job job) {
        if (job == null || job.getId() == null) {
            return Optional.empty();
        }
        return repository.findByJobId(job.getId());
    }

    @Transactional(readOnly = true)
    public Map<Long, JobDetail> findByJobIds(Collection<Long> jobIds) {
        if (jobIds == null || jobIds.isEmpty()) {
            return Map.of();
        }
        Set<Long> distinctIds = jobIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        return repository.findAllByJobIds(distinctIds).stream()
                .filter(detail -> detail.getJob() != null && detail.getJob().getId() != null)
                .collect(Collectors.toMap(detail -> detail.getJob().getId(), detail -> detail,
                        (existing, replacement) -> existing));
    }

    @Transactional(readOnly = true)
    public Map<Long, String> findContentTextByJobIds(Collection<Long> jobIds) {
        if (jobIds == null || jobIds.isEmpty()) {
            return Map.of();
        }
        Set<Long> distinctIds = jobIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        return repository.findContentTextByJobIds(distinctIds).stream()
                .collect(Collectors.toMap(JobDetailRepository.ContentTextView::getJobId,
                        JobDetailRepository.ContentTextView::getContentText));
    }

    private boolean applyEnrichment(JobDetail detail, JobContentEnrichment enrichment) {
        boolean updated = false;
        String normalizedSummary = trimToNull(enrichment.summary());
        if (!Objects.equals(trimToNull(detail.getSummary()), normalizedSummary)) {
            detail.setSummary(normalizedSummary);
            updated = true;
        }

        String normalizedStructured = trimToNull(enrichment.structuredData());
        if (!Objects.equals(trimToNull(detail.getStructuredData()), normalizedStructured)) {
            detail.setStructuredData(normalizedStructured);
            updated = true;
        }

        if (detail.replaceSkills(enrichment.skills() != null ? enrichment.skills() : Collections.emptyList())) {
            updated = true;
        }

        if (detail.replaceHighlights(enrichment.highlights() != null ? enrichment.highlights() : Collections.emptyList())) {
            updated = true;
        }

        return updated;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

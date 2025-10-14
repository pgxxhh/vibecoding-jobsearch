package com.vibe.jobs.service;

import com.vibe.jobs.domain.Job;
import com.vibe.jobs.domain.JobDetail;
import com.vibe.jobs.domain.JobEnrichmentKey;
import com.vibe.jobs.repo.JobDetailRepository;
import com.vibe.jobs.service.HtmlTextExtractor;
import com.vibe.jobs.service.JobContentFingerprintCalculator;
import com.vibe.jobs.service.enrichment.JobDetailContentUpdatedEvent;
import com.vibe.jobs.service.enrichment.JobSnapshot;
import com.vibe.jobs.service.dto.JobDetailEnrichmentsDto;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JobDetailService {

    private final JobDetailRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final JobContentFingerprintCalculator fingerprintCalculator;

    public JobDetailService(JobDetailRepository repository,
                            ApplicationEventPublisher eventPublisher,
                            JobContentFingerprintCalculator fingerprintCalculator) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.fingerprintCalculator = fingerprintCalculator;
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

        boolean isNew = detail.getId() == null;
        boolean changed = isNew;
        boolean contentChanged = isNew;

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
            detail.incrementContentVersion();
        }

        if (changed) {
            repository.save(detail);
        }

        if (contentChanged) {
            long newVersion = detail.getContentVersion();
            String fingerprint = fingerprintCalculator.compute(jobId, contentText);
            JobSnapshot snapshot = JobSnapshot.from(job);
            eventPublisher.publishEvent(new JobDetailContentUpdatedEvent(
                    detail.getId(),
                    jobId,
                    snapshot,
                    content,
                    contentText,
                    newVersion,
                    fingerprint
            ));
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
    public Map<Long, JobDetailEnrichmentsDto> findByJobIds(Collection<Long> jobIds) {
        if (jobIds == null || jobIds.isEmpty()) {
            return Map.of();
        }
        Set<Long> distinctIds = jobIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, EnumMap<JobEnrichmentKey, String>> aggregated = new HashMap<>();
        repository.findEnrichmentsByJobIds(distinctIds)
                .forEach(view -> {
                    Long jobId = view.getJobId();
                    if (jobId == null) {
                        return;
                    }
                    EnumMap<JobEnrichmentKey, String> values = aggregated.computeIfAbsent(jobId,
                            id -> new EnumMap<>(JobEnrichmentKey.class));
                    JobEnrichmentKey key = view.getEnrichmentKey();
                    if (key != null) {
                        values.put(key, view.getValueJson());
                    }
                });

        return aggregated.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> new JobDetailEnrichmentsDto(entry.getKey(), entry.getValue())));
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

    @Transactional(readOnly = true)
    public Set<Long> findMatchingJobIds(Collection<Long> jobIds, String query) {
        if (jobIds == null || jobIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> distinctIds = jobIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .collect(Collectors.toSet());
        if (distinctIds.isEmpty()) {
            return Set.of();
        }
        return repository.findMatchingJobIds(distinctIds, query);
    }

}

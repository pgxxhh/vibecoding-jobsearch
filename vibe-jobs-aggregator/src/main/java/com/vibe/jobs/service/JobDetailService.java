package com.vibe.jobs.service;

import com.vibe.jobs.domain.Job;
import com.vibe.jobs.domain.JobDetail;
import com.vibe.jobs.repo.JobDetailRepository;
import com.vibe.jobs.service.HtmlTextExtractor;
import com.vibe.jobs.service.enrichment.JobDetailContentUpdatedEvent;
import com.vibe.jobs.service.enrichment.JobSnapshot;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
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

    public JobDetailService(JobDetailRepository repository,
                            ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
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
            detail.incrementContentVersion();
        }

        if (changed) {
            repository.save(detail);
        }

        if (contentChanged) {
            long newVersion = detail.getContentVersion();
            String fingerprint = computeFingerprint(jobId, contentText);
            JobSnapshot snapshot = toSnapshot(job);
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

    private JobSnapshot toSnapshot(Job job) {
        List<String> tags = job.getTags() == null ? List.of() : List.copyOf(job.getTags());
        return new JobSnapshot(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getLocation(),
                job.getLevel(),
                job.getUrl(),
                tags
        );
    }

    private String computeFingerprint(Long jobId, String contentText) {
        String source = (jobId != null ? jobId : 0L) + ":" + (contentText != null ? contentText : "");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}

package com.vibe.jobs.service;

import com.vibe.jobs.domain.Job;
import com.vibe.jobs.domain.JobDetail;
import com.vibe.jobs.repo.JobDetailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JobDetailService {

    private final JobDetailRepository repository;

    public JobDetailService(JobDetailRepository repository) {
        this.repository = repository;
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
        if (!content.equals(detail.getContent())) {
            detail.setContent(content);
            changed = true;
        }

        if (!Objects.equals(contentText, detail.getContentText())) {
            detail.setContentText(contentText);
            changed = true;
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
}

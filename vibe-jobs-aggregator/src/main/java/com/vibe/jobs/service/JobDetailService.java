package com.vibe.jobs.service;

import com.vibe.jobs.domain.Job;
import com.vibe.jobs.domain.JobDetail;
import com.vibe.jobs.repo.JobDetailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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

        JobDetail detail = repository.findByJobId(jobId)
                .orElseGet(() -> new JobDetail(job, content));

        boolean changed = detail.getId() == null;
        if (!content.equals(detail.getContent())) {
            detail.setContent(content);
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
}

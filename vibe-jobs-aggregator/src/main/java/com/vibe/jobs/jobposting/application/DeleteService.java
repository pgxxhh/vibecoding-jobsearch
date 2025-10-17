package com.vibe.jobs.jobposting.application;

import com.vibe.jobs.jobposting.infrastructure.persistence.JobDetailRepository;
import com.vibe.jobs.jobposting.infrastructure.persistence.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 统一的删除服务
 */
@Service
public class DeleteService {

    private final JobRepository jobRepository;
    private final JobDetailRepository jobDetailRepository;

    public DeleteService(JobRepository jobRepository, JobDetailRepository jobDetailRepository) {
        this.jobRepository = jobRepository;
        this.jobDetailRepository = jobDetailRepository;
    }

    /**
     * 删除Job及其关联的JobDetail
     */
    @Transactional
    public void deleteJob(Long jobId) {
        Instant now = Instant.now();
        jobRepository.softDeleteById(jobId, now);
        jobDetailRepository.softDeleteByJobId(jobId, now);
    }

    /**
     * 批量删除Jobs
     */
    @Transactional
    public void deleteJobs(List<Long> jobIds) {
        Instant now = Instant.now();
        jobRepository.softDeleteByIds(jobIds, now);
        for (Long jobId : jobIds) {
            jobDetailRepository.softDeleteByJobId(jobId, now);
        }
    }

    /**
     * 删除JobDetail
     */
    @Transactional
    public void deleteJobDetail(Long jobDetailId) {
        Instant now = Instant.now();
        jobDetailRepository.softDeleteById(jobDetailId, now);
    }
}
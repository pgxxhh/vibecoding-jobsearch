package com.vibe.jobs.jobposting.infrastructure.persistence;

import com.vibe.jobs.jobposting.domain.Job;
import com.vibe.jobs.jobposting.domain.spi.JobRepositoryPort;
import com.vibe.jobs.jobposting.infrastructure.persistence.entity.JobJpaEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class JobRepositoryAdapter implements JobRepositoryPort {

    private final JobJpaRepository jobJpaRepository;

    public JobRepositoryAdapter(JobJpaRepository jobJpaRepository) {
        this.jobJpaRepository = jobJpaRepository;
    }

    @Override
    @Transactional
    public Job save(Job job) {
        JobJpaEntity entity;
        if (job.getId() != null) {
            entity = jobJpaRepository.findById(job.getId())
                    .orElseGet(() -> JobJpaEntity.fromDomain(job));
            entity.updateFromDomain(job);
        } else {
            entity = JobJpaEntity.fromDomain(job);
        }
        JobJpaEntity saved = jobJpaRepository.save(entity);
        Job mapped = saved.toDomain();
        copyJobState(mapped, job);
        return mapped;
    }

    @Override
    public Optional<Job> findById(Long id) {
        return jobJpaRepository.findById(id).map(JobJpaEntity::toDomain);
    }

    @Override
    public Optional<Job> findByIdIncludingDeleted(Long id) {
        return jobJpaRepository.findByIdIncludingDeleted(id).map(JobJpaEntity::toDomain);
    }

    @Override
    public Optional<Job> findBySourceAndExternalId(String source, String externalId) {
        return jobJpaRepository.findBySourceAndExternalId(source, externalId).map(JobJpaEntity::toDomain);
    }

    @Override
    public Optional<Job> findMostRecentByCompanyAndTitleIgnoreCase(String company, String title) {
        return jobJpaRepository
                .findFirstByDeletedFalseAndCompanyIgnoreCaseAndTitleIgnoreCaseOrderByCreatedAtDesc(company, title)
                .map(JobJpaEntity::toDomain);
    }

    @Override
    @Transactional
    public void softDeleteById(Long id, Instant deletedAt) {
        jobJpaRepository.softDeleteById(id, deletedAt);
    }

    @Override
    @Transactional
    public void softDeleteByIds(List<Long> ids, Instant deletedAt) {
        jobJpaRepository.softDeleteByIds(ids, deletedAt);
    }

    @Override
    public List<Job> searchAfter(String q,
                                 String company,
                                 String location,
                                 String level,
                                 Instant postedAfter,
                                 Instant cursorPostedAt,
                                 Long cursorId,
                                 boolean searchDetail,
                                 int offset,
                                 int limit) {
        List<JobJpaEntity> entities = jobJpaRepository.searchAfter(q, company, location, level, postedAfter, cursorPostedAt,
                cursorId, searchDetail, offset, limit);
        List<Job> jobs = new ArrayList<>(entities.size());
        for (JobJpaEntity entity : entities) {
            jobs.add(entity.toDomain());
        }
        return jobs;
    }

    @Override
    public long countSearch(String q,
                            String company,
                            String location,
                            String level,
                            Instant postedAfter,
                            boolean searchDetail) {
        return jobJpaRepository.countSearch(q, company, location, level, postedAfter, searchDetail);
    }

    private void copyJobState(Job source, Job target) {
        if (target == null) {
            return;
        }
        target.setId(source.getId());
        target.setSource(source.getSource());
        target.setExternalId(source.getExternalId());
        target.setTitle(source.getTitle());
        target.setCompany(source.getCompany());
        target.setLocation(source.getLocation());
        target.setLevel(source.getLevel());
        target.setPostedAt(source.getPostedAt());
        target.setTags(source.getTags() != null ? new java.util.HashSet<>(source.getTags()) : new java.util.HashSet<>());
        target.setUrl(source.getUrl());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        target.setChecksum(source.getChecksum());
        target.setDeleted(source.isDeleted());
    }
}

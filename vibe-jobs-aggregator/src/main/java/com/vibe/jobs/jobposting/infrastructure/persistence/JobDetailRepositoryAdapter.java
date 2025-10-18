package com.vibe.jobs.jobposting.infrastructure.persistence;

import com.vibe.jobs.jobposting.domain.JobDetail;
import com.vibe.jobs.jobposting.domain.JobDetailEnrichment;
import com.vibe.jobs.jobposting.domain.spi.JobDetailRepositoryPort;
import com.vibe.jobs.jobposting.infrastructure.persistence.entity.JobDetailJpaEntity;
import com.vibe.jobs.jobposting.infrastructure.persistence.entity.JobJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@Transactional(readOnly = true)
public class JobDetailRepositoryAdapter implements JobDetailRepositoryPort {

    private final JobDetailJpaRepository jobDetailJpaRepository;
    private final JobJpaRepository jobJpaRepository;

    public JobDetailRepositoryAdapter(JobDetailJpaRepository jobDetailJpaRepository,
                                      JobJpaRepository jobJpaRepository) {
        this.jobDetailJpaRepository = jobDetailJpaRepository;
        this.jobJpaRepository = jobJpaRepository;
    }

    @Override
    @Transactional
    public JobDetail save(JobDetail detail) {
        if (detail == null || detail.getJob() == null || detail.getJob().getId() == null) {
            throw new IllegalArgumentException("Job detail must be associated with a persisted job");
        }
        JobJpaEntity jobEntity = jobJpaRepository.getReferenceById(detail.getJob().getId());
        JobDetailJpaEntity entity;
        if (detail.getId() != null) {
            entity = jobDetailJpaRepository.findById(detail.getId())
                    .orElseGet(() -> JobDetailJpaEntity.fromDomain(detail, jobEntity));
            entity.updateFromDomain(detail, jobEntity);
        } else {
            entity = JobDetailJpaEntity.fromDomain(detail, jobEntity);
        }
        JobDetailJpaEntity saved = jobDetailJpaRepository.save(entity);
        JobDetail mapped = saved.toDomain();
        copyDetailState(mapped, detail);
        return mapped;
    }

    @Override
    public Optional<JobDetail> findById(Long id) {
        return jobDetailJpaRepository.findById(id).map(JobDetailJpaEntity::toDomain);
    }

    @Override
    public Optional<JobDetail> findByIdWithEnrichments(Long id) {
        return jobDetailJpaRepository.findByIdWithEnrichments(id).map(JobDetailJpaEntity::toDomain);
    }

    @Override
    public Optional<JobDetail> findByJobId(Long jobId) {
        return jobDetailJpaRepository.findByJobId(jobId).map(JobDetailJpaEntity::toDomain);
    }

    @Override
    public List<JobDetailContentText> findContentTextByJobIds(Collection<Long> jobIds) {
        return jobDetailJpaRepository.findContentTextByJobIds(jobIds).stream()
                .map(view -> new JobDetailContentText(view.getJobId(), view.getContentText()))
                .collect(Collectors.toList());
    }

    @Override
    public List<JobDetailEnrichmentView> findEnrichmentsByJobIds(Collection<Long> jobIds) {
        return jobDetailJpaRepository.findEnrichmentsByJobIds(jobIds).stream()
                .map(view -> new JobDetailEnrichmentView(view.getJobId(), view.getEnrichmentKey(), view.getValueJson()))
                .collect(Collectors.toList());
    }

    @Override
    public Set<Long> findMatchingJobIds(Collection<Long> jobIds, String query) {
        return jobDetailJpaRepository.findMatchingJobIds(jobIds, query);
    }

    @Override
    @Transactional
    public void softDeleteById(Long id, Instant deletedAt) {
        jobDetailJpaRepository.softDeleteById(id, deletedAt);
    }

    @Override
    @Transactional
    public void softDeleteByJobId(Long jobId, Instant deletedAt) {
        jobDetailJpaRepository.softDeleteByJobId(jobId, deletedAt);
    }

    @Override
    public Optional<JobDetail> findByIdIncludingDeleted(Long id) {
        return jobDetailJpaRepository.findByIdIncludingDeleted(id).map(JobDetailJpaEntity::toDomain);
    }

    @Override
    public JobDetailPage fetchPageOrderedById(int pageNumber, int pageSize) {
        Page<JobDetailJpaEntity> page = jobDetailJpaRepository.findAll(PageRequest.of(pageNumber, pageSize,
                Sort.by(Sort.Direction.ASC, "id")));
        List<JobDetail> content = page.getContent().stream()
                .map(JobDetailJpaEntity::toDomain)
                .collect(Collectors.toList());
        return new JobDetailPage(content, page.hasNext());
    }

    @Override
    @Transactional
    public void saveAll(Collection<JobDetail> details) {
        if (details == null || details.isEmpty()) {
            return;
        }
        List<JobDetailJpaEntity> entities = new ArrayList<>(details.size());
        List<JobDetail> originals = new ArrayList<>(details.size());
        for (JobDetail detail : details) {
            if (detail == null || detail.getJob() == null || detail.getJob().getId() == null) {
                continue;
            }
            JobJpaEntity jobEntity = jobJpaRepository.getReferenceById(detail.getJob().getId());
            JobDetailJpaEntity entity = detail.getId() != null
                    ? jobDetailJpaRepository.findById(detail.getId())
                    .orElseGet(() -> JobDetailJpaEntity.fromDomain(detail, jobEntity))
                    : JobDetailJpaEntity.fromDomain(detail, jobEntity);
            entity.updateFromDomain(detail, jobEntity);
            entities.add(entity);
            originals.add(detail);
        }
        if (entities.isEmpty()) {
            return;
        }
        List<JobDetailJpaEntity> savedEntities = jobDetailJpaRepository.saveAll(entities);
        for (int i = 0; i < savedEntities.size(); i++) {
            JobDetailJpaEntity saved = savedEntities.get(i);
            JobDetail target = originals.get(i);
            JobDetail mapped = saved.toDomain();
            copyDetailState(mapped, target);
        }
    }

    private void copyDetailState(JobDetail source, JobDetail target) {
        if (target == null) {
            return;
        }
        target.setId(source.getId());
        target.setJob(source.getJob());
        target.setContent(source.getContent());
        target.setContentText(source.getContentText());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        target.setDeleted(source.isDeleted());
        target.setContentVersion(source.getContentVersion());
        if (source.getEnrichments() != null) {
            Set<JobDetailEnrichment> mapped = source.getEnrichments().stream()
                    .map(enrichment -> {
                        enrichment.setJobDetail(target);
                        return enrichment;
                    })
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            target.setEnrichments(mapped);
        } else {
            target.setEnrichments(new LinkedHashSet<>());
        }
    }
}

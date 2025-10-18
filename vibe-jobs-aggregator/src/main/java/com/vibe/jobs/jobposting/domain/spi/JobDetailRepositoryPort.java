package com.vibe.jobs.jobposting.domain.spi;

import com.vibe.jobs.jobposting.domain.JobDetail;
import com.vibe.jobs.jobposting.domain.JobEnrichmentKey;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface JobDetailRepositoryPort {

    JobDetail save(JobDetail detail);

    Optional<JobDetail> findById(Long id);

    Optional<JobDetail> findByIdWithEnrichments(Long id);

    Optional<JobDetail> findByJobId(Long jobId);

    List<JobDetailContentText> findContentTextByJobIds(Collection<Long> jobIds);

    List<JobDetailEnrichmentView> findEnrichmentsByJobIds(Collection<Long> jobIds);

    Set<Long> findMatchingJobIds(Collection<Long> jobIds, String query);

    void softDeleteById(Long id, Instant deletedAt);

    void softDeleteByJobId(Long jobId, Instant deletedAt);

    Optional<JobDetail> findByIdIncludingDeleted(Long id);

    JobDetailPage fetchPageOrderedById(int pageNumber, int pageSize);

    void saveAll(Collection<JobDetail> details);

    record JobDetailContentText(Long jobId, String contentText) {
    }

    record JobDetailEnrichmentView(Long jobId, JobEnrichmentKey enrichmentKey, String valueJson) {
    }

    record JobDetailPage(List<JobDetail> content, boolean hasNext) {
    }
}

package com.vibe.jobs.jobposting.domain.spi;

import com.vibe.jobs.jobposting.domain.Job;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JobRepositoryPort {

    Job save(Job job);

    Optional<Job> findById(Long id);

    Optional<Job> findByIdIncludingDeleted(Long id);

    Optional<Job> findBySourceAndExternalId(String source, String externalId);

    Optional<Job> findMostRecentByCompanyAndTitleIgnoreCase(String company, String title);

    void softDeleteById(Long id, Instant deletedAt);

    void softDeleteByIds(List<Long> ids, Instant deletedAt);

    List<Job> searchAfter(String q,
                          String company,
                          String location,
                          String level,
                          Instant postedAfter,
                          Instant cursorPostedAt,
                          Long cursorId,
                          boolean searchDetail,
                          int offset,
                          int limit);

    long countSearch(String q,
                     String company,
                     String location,
                     String level,
                     Instant postedAfter,
                     boolean searchDetail);
}

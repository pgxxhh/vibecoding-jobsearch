package com.vibe.jobs.jobposting.infrastructure.persistence;

import com.vibe.jobs.jobposting.infrastructure.persistence.entity.JobJpaEntity;

import java.time.Instant;
import java.util.List;

public interface JobJpaRepositoryCustom {

    List<JobJpaEntity> searchAfter(String q,
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

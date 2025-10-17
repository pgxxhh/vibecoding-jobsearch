package com.vibe.jobs.jobposting.infrastructure.persistence;

import com.vibe.jobs.jobposting.domain.Job;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface JobRepositoryCustom {
    List<Job> searchAfter(String q,
                          String company,
                          String location,
                          String level,
                          Instant postedAfter,
                          Instant cursorPostedAt,
                          Long cursorId,
                          boolean searchDetail,
                          Pageable pageable);

    long countSearch(String q,
                     String company,
                     String location,
                     String level,
                     Instant postedAfter,
                     boolean searchDetail);
}

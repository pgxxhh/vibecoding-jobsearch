
package com.vibe.jobs.repo;

import com.vibe.jobs.domain.Job;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    Job findBySourceAndExternalId(String source, String externalId);

    @Query("""
        select j from Job j
        where (:q is null or lower(j.title) like lower(concat('%',:q,'%')) or exists (
                select t from j.tags t where lower(t) like lower(concat('%',:q,'%'))
            ))
        and (:company is null or lower(j.company) like lower(concat('%',:company,'%')))
        and (:location is null or lower(j.location) like lower(concat('%',:location,'%')))
        and (:level is null or lower(j.level) = lower(:level))
        and (:postedAfter is null or j.postedAt >= :postedAfter)
        and (
            :cursorPostedAt is null or
            j.postedAt < :cursorPostedAt or
            (j.postedAt = :cursorPostedAt and j.id < :cursorId)
        )
        """)
    List<Job> searchAfter(@Param("q") String q,
                          @Param("company") String company,
                          @Param("location") String location,
                          @Param("level") String level,
                          @Param("postedAfter") Instant postedAfter,
                          @Param("cursorPostedAt") Instant cursorPostedAt,
                          @Param("cursorId") Long cursorId,
                          Pageable pageable);

    @Query("""
        select count(j) from Job j
        where (:q is null or lower(j.title) like lower(concat('%',:q,'%')) or exists (
                select t from j.tags t where lower(t) like lower(concat('%',:q,'%'))
            ))
        and (:company is null or lower(j.company) like lower(concat('%',:company,'%')))
        and (:location is null or lower(j.location) like lower(concat('%',:location,'%')))
        and (:level is null or lower(j.level) = lower(:level))
        and (:postedAfter is null or j.postedAt >= :postedAfter)
        """)
    long countSearch(@Param("q") String q,
                     @Param("company") String company,
                     @Param("location") String location,
                     @Param("level") String level,
                     @Param("postedAfter") Instant postedAfter);
}

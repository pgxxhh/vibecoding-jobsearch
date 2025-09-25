
package com.vibe.jobs.repo;

import com.vibe.jobs.domain.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
        """)
    Page<Job> search(@Param("q") String q,
                     @Param("company") String company,
                     @Param("location") String location,
                     @Param("level") String level,
                     Pageable pageable);
}

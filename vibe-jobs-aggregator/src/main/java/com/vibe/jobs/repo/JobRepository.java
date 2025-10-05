
package com.vibe.jobs.repo;

import com.vibe.jobs.domain.Job;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {
    
    @Query("SELECT j FROM Job j WHERE j.source = :source AND j.externalId = :externalId AND j.deleted = false")
    Job findBySourceAndExternalId(@Param("source") String source, @Param("externalId") String externalId);

    @Query("""
        select j from Job j
        where j.deleted = false
        and (:q is null or 
               lower(j.title) like lower(concat('%',:q,'%')) or 
               lower(j.company) like lower(concat('%',:company,'%')) or 
               lower(j.location) like lower(concat('%',:location,'%')) or 
               exists (select t from j.tags t where lower(t) like lower(concat('%',:q,'%')))
            )
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
        select j from Job j
        where j.deleted = false
        and (:q is null or
               lower(j.title) like lower(concat('%',:q,'%')) or
               lower(j.company) like lower(concat('%',:company,'%')) or
               lower(j.location) like lower(concat('%',:location,'%')) or
               exists (select t from j.tags t where lower(t) like lower(concat('%',:q,'%')))
            )
        and (:company is null or lower(j.company) like lower(concat('%',:company,'%')))
        and (:location is null or lower(j.location) like lower(concat('%',:location,'%')))
        and (:level is null or lower(j.level) = lower(:level))
        and j.postedAt is not null
        and (
            :sincePostedAt is null or
            j.postedAt > :sincePostedAt or
            (j.postedAt = :sincePostedAt and j.id > :sinceId)
        )
        order by j.postedAt asc, j.id asc
        """)
    List<Job> findNewJobsForAlert(@Param("q") String q,
                                  @Param("company") String company,
                                  @Param("location") String location,
                                  @Param("level") String level,
                                  @Param("sincePostedAt") Instant sincePostedAt,
                                  @Param("sinceId") Long sinceId,
                                  Pageable pageable);

    @Query("""
        select count(j) from Job j
        where j.deleted = false
        and (:q is null or 
               lower(j.title) like lower(concat('%',:q,'%')) or 
               lower(j.company) like lower(concat('%',:company,'%')) or 
               lower(j.location) like lower(concat('%',:location,'%')) or 
               exists (select t from j.tags t where lower(t) like lower(concat('%',:q,'%')))
            )
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

    @Query("SELECT j FROM Job j WHERE j.deleted = false AND lower(j.company) = lower(:company) AND lower(j.title) = lower(:title) ORDER BY j.createdAt DESC")
    Optional<Job> findTopByCompanyIgnoreCaseAndTitleIgnoreCase(@Param("company") String company, @Param("title") String title);

    // 软删除方法 - 避免与JpaRepository的deleteById冲突
    @Modifying
    @Query("UPDATE Job j SET j.deleted = true, j.updatedAt = :deletedAt WHERE j.id = :id")
    void softDeleteById(@Param("id") Long id, @Param("deletedAt") Instant deletedAt);

    // 批量软删除
    @Modifying
    @Query("UPDATE Job j SET j.deleted = true, j.updatedAt = :deletedAt WHERE j.id IN :ids")
    void softDeleteByIds(@Param("ids") List<Long> ids, @Param("deletedAt") Instant deletedAt);

    // 查找所有（包括软删除的）
    @Query("SELECT j FROM Job j WHERE j.id = :id")
    Optional<Job> findByIdIncludingDeleted(@Param("id") Long id);
}

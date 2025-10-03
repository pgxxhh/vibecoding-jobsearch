package com.vibe.jobs.datasource.infrastructure.jpa;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SpringDataJobDataSourceRepository extends JpaRepository<JobDataSourceEntity, Long> {

    @EntityGraph(attributePaths = {"companies", "categories"})
    @Query("select distinct ds from JobDataSourceEntity ds where ds.enabled = true")
    List<JobDataSourceEntity> findAllEnabled();

    @Override
    @EntityGraph(attributePaths = {"companies", "categories"})
    List<JobDataSourceEntity> findAll();

    boolean existsByCode(String code);
}

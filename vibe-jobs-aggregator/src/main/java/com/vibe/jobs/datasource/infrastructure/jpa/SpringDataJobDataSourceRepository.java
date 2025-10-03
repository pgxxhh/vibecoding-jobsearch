package com.vibe.jobs.datasource.infrastructure.jpa;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SpringDataJobDataSourceRepository extends JpaRepository<JobDataSourceEntity, Long> {

    @Query("select ds from JobDataSourceEntity ds where ds.enabled = true")
    List<JobDataSourceEntity> findAllEnabled();

    @Override
    List<JobDataSourceEntity> findAll();

    boolean existsByCode(String code);

    Optional<JobDataSourceEntity> findByCode(String code);
}

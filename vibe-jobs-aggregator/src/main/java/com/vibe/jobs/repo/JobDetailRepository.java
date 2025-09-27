package com.vibe.jobs.repo;

import com.vibe.jobs.domain.JobDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobDetailRepository extends JpaRepository<JobDetail, Long> {
    Optional<JobDetail> findByJobId(Long jobId);
}

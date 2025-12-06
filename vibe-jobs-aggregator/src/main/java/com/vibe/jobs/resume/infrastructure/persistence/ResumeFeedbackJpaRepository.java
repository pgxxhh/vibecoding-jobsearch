package com.vibe.jobs.resume.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeFeedbackJpaRepository extends JpaRepository<ResumeFeedbackEntity, Long> {
}

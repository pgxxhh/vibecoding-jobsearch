package com.vibe.jobs.subscription.repo;

import com.vibe.jobs.subscription.domain.JobAlertSubscription;
import com.vibe.jobs.subscription.domain.JobAlertSubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobAlertSubscriptionRepository extends JpaRepository<JobAlertSubscription, Long> {
    List<JobAlertSubscription> findByUserId(UUID userId);

    long countByUserIdAndStatusNot(UUID userId, JobAlertSubscriptionStatus status);

    Optional<JobAlertSubscription> findByIdAndUserId(Long id, UUID userId);

    List<JobAlertSubscription> findByStatus(JobAlertSubscriptionStatus status);
}

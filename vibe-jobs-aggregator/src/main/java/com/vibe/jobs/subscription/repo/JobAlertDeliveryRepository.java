package com.vibe.jobs.subscription.repo;

import com.vibe.jobs.subscription.domain.JobAlertDelivery;
import com.vibe.jobs.subscription.domain.JobAlertSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobAlertDeliveryRepository extends JpaRepository<JobAlertDelivery, Long> {
    Optional<JobAlertDelivery> findTopBySubscriptionOrderByDeliveredAtDesc(JobAlertSubscription subscription);
}

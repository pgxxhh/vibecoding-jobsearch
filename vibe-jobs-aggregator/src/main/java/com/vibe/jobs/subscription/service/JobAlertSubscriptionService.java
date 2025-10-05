package com.vibe.jobs.subscription.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.jobs.auth.domain.EmailAddress;
import com.vibe.jobs.domain.Job;
import com.vibe.jobs.repo.JobRepository;
import com.vibe.jobs.subscription.config.JobAlertProperties;
import com.vibe.jobs.subscription.domain.JobAlertCursor;
import com.vibe.jobs.subscription.domain.JobAlertDelivery;
import com.vibe.jobs.subscription.domain.JobAlertDeliveryStatus;
import com.vibe.jobs.subscription.domain.JobAlertSubscription;
import com.vibe.jobs.subscription.domain.JobAlertSubscriptionStatus;
import com.vibe.jobs.subscription.email.JobAlertEmailSender;
import com.vibe.jobs.subscription.repo.JobAlertDeliveryRepository;
import com.vibe.jobs.subscription.repo.JobAlertSubscriptionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class JobAlertSubscriptionService {
    private final JobAlertSubscriptionRepository subscriptionRepository;
    private final JobAlertDeliveryRepository deliveryRepository;
    private final JobRepository jobRepository;
    private final JobAlertEmailSender emailSender;
    private final JobAlertProperties properties;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public JobAlertSubscriptionService(JobAlertSubscriptionRepository subscriptionRepository,
                                       JobAlertDeliveryRepository deliveryRepository,
                                       JobRepository jobRepository,
                                       JobAlertEmailSender emailSender,
                                       JobAlertProperties properties,
                                       Optional<Clock> clock,
                                       ObjectMapper objectMapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.deliveryRepository = deliveryRepository;
        this.jobRepository = jobRepository;
        this.emailSender = emailSender;
        this.properties = properties;
        this.clock = clock.orElse(Clock.systemUTC());
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<JobAlertSubscription> listFor(UUID userId) {
        return subscriptionRepository.findByUserId(userId);
    }

    public JobAlertSubscription create(UUID userId, String email, CreateJobAlertCommand command) {
        enforceLimit(userId);
        JobAlertSubscription subscription = new JobAlertSubscription();
        subscription.setUserId(userId);
        subscription.setEmail(email);
        subscription.setSearchKeyword(trimToNull(command.keyword()));
        subscription.setCompany(trimToNull(command.company()));
        subscription.setLocation(trimToNull(command.location()));
        subscription.setLevel(trimToNull(command.level()));
        subscription.setFiltersJson(serializeFilters(command.filters()));
        subscription.setScheduleHour(normalizeHour(command.scheduleHour()));
        subscription.setTimezone(resolveTimezone(command.timezone()));
        subscription.setStatus(JobAlertSubscriptionStatus.ACTIVE);
        subscription.setLastSeenCursor(JobAlertCursor.fromInstant(Instant.now(clock)).encode());
        return subscriptionRepository.save(subscription);
    }

    public JobAlertSubscription update(UUID userId, Long id, UpdateJobAlertCommand command) {
        JobAlertSubscription subscription = subscriptionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("Subscription not found"));
        if (command.keyword() != null) subscription.setSearchKeyword(trimToNull(command.keyword()));
        if (command.company() != null) subscription.setCompany(trimToNull(command.company()));
        if (command.location() != null) subscription.setLocation(trimToNull(command.location()));
        if (command.level() != null) subscription.setLevel(trimToNull(command.level()));
        if (command.filters() != null) subscription.setFiltersJson(serializeFilters(command.filters()));
        if (command.scheduleHour() != null) subscription.setScheduleHour(normalizeHour(command.scheduleHour()));
        if (command.timezone() != null) subscription.setTimezone(resolveTimezone(command.timezone()));
        if (command.status() != null) subscription.setStatus(command.status());
        return subscriptionRepository.save(subscription);
    }

    public void cancel(UUID userId, Long id) {
        JobAlertSubscription subscription = requireOwnedSubscription(userId, id);
        subscription.setStatus(JobAlertSubscriptionStatus.CANCELLED);
        subscriptionRepository.save(subscription);
    }

    public void unsubscribe(Long subscriptionId, String token) {
        JobAlertSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found"));
        if (!isValidToken(subscription, token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid unsubscribe token");
        }
        subscription.setStatus(JobAlertSubscriptionStatus.CANCELLED);
        subscriptionRepository.save(subscription);
    }

    @Transactional(readOnly = true)
    public Optional<JobAlertSubscription> findById(Long id) {
        return subscriptionRepository.findById(id);
    }

    public void triggerTest(UUID userId, Long subscriptionId) {
        JobAlertSubscription subscription = requireOwnedSubscription(userId, subscriptionId);
        var digest = new JobAlertDigest(buildSummary(subscription),
                buildUnsubscribeUrl(subscription),
                List.of());
        emailSender.sendDigest(new EmailAddress(subscription.getEmail()), digest);
    }

    public boolean shouldRun(JobAlertSubscription subscription, Instant now) {
        if (subscription.getStatus() != JobAlertSubscriptionStatus.ACTIVE) {
            return false;
        }
        ZoneId zone = ZoneId.of(resolveTimezone(subscription.getTimezone()));
        ZonedDateTime localNow = now.atZone(zone);
        if (localNow.getHour() != subscription.getScheduleHour()) {
            return false;
        }
        Optional<JobAlertDelivery> lastDelivery = deliveryRepository.findTopBySubscriptionOrderByDeliveredAtDesc(subscription);
        if (lastDelivery.isEmpty()) {
            return true;
        }
        JobAlertDelivery delivery = lastDelivery.get();
        if (delivery.getStatus() == JobAlertDeliveryStatus.FAILED) {
            Instant nextAttempt = delivery.getDeliveredAt().plus(Duration.ofMinutes(15));
            return nextAttempt.isBefore(now) || nextAttempt.equals(now);
        }
        ZonedDateTime lastLocal = delivery.getDeliveredAt().atZone(zone);
        return !lastLocal.toLocalDate().isEqual(localNow.toLocalDate());
    }

    public void processSubscription(JobAlertSubscription subscription) {
        Instant now = Instant.now(clock);
        JobAlertCursor cursor = JobAlertCursor.decode(subscription.getLastSeenCursor());
        Instant sincePostedAt = cursor != null ? cursor.postedAt() : now.minus(properties.getSchedulerLookback());
        Long sinceId = cursor != null ? cursor.jobId() : 0L;

        var pageable = PageRequest.of(0, properties.getDigestMaxJobs(), Sort.by(Sort.Direction.ASC, "postedAt", "id"));
        List<Job> jobs = jobRepository.findNewJobsForAlert(
                subscription.getSearchKeyword(),
                subscription.getCompany(),
                subscription.getLocation(),
                subscription.getLevel(),
                sincePostedAt,
                sinceId,
                pageable
        );

        if (jobs.isEmpty()) {
            recordDelivery(subscription, now, JobAlertDeliveryStatus.SKIPPED, List.of(), null);
            subscription.setLastSeenCursor(JobAlertCursor.fromInstant(now).encode());
            subscriptionRepository.save(subscription);
            return;
        }

        Job lastJob = jobs.get(jobs.size() - 1);
        JobAlertCursor newCursor = new JobAlertCursor(lastJob.getPostedAt(), lastJob.getId());

        JobAlertDigest digest = new JobAlertDigest(
                buildSummary(subscription),
                buildUnsubscribeUrl(subscription),
                jobs.stream().map(job -> new JobAlertDigest.JobItem(
                        job.getId(),
                        job.getTitle(),
                        job.getCompany(),
                        job.getLocation(),
                        job.getPostedAt(),
                        job.getUrl()
                )).collect(Collectors.toList())
        );

        try {
            emailSender.sendDigest(new EmailAddress(subscription.getEmail()), digest).join();
            subscription.setLastNotifiedAt(now);
            subscription.setLastSeenCursor(newCursor.encode());
            subscriptionRepository.save(subscription);
            recordDelivery(subscription, now, JobAlertDeliveryStatus.SENT, jobs.stream().map(Job::getId).toList(), null);
        } catch (Exception ex) {
            recordDelivery(subscription, now, JobAlertDeliveryStatus.FAILED, jobs.stream().map(Job::getId).toList(), ex.getMessage());
        }
    }

    private void recordDelivery(JobAlertSubscription subscription,
                                 Instant deliveredAt,
                                 JobAlertDeliveryStatus status,
                                 List<Long> jobIds,
                                 String errorMessage) {
        JobAlertDelivery delivery = new JobAlertDelivery();
        delivery.setSubscription(subscription);
        delivery.setDeliveredAt(deliveredAt);
        delivery.setStatus(status);
        delivery.setJobCount(jobIds.size());
        delivery.setJobIds(serializeJobIds(jobIds));
        delivery.setErrorMessage(errorMessage);
        deliveryRepository.save(delivery);
    }

    private String serializeJobIds(List<Long> jobIds) {
        try {
            return objectMapper.writeValueAsString(jobIds);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize job ids", e);
        }
    }

    private void enforceLimit(UUID userId) {
        long count = subscriptionRepository.countByUserIdAndStatusNot(userId, JobAlertSubscriptionStatus.CANCELLED);
        if (count >= properties.getMaxSubscriptionsPerUser()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subscription limit exceeded");
        }
    }

    private JobAlertSubscription requireOwnedSubscription(UUID userId, Long id) {
        return subscriptionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found"));
    }

    private int normalizeHour(Integer hour) {
        if (hour == null) {
            return properties.getDefaultScheduleHour();
        }
        int value = hour % 24;
        return value < 0 ? value + 24 : value;
    }

    private String resolveTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return properties.getDefaultTimezone();
        }
        try {
            ZoneId.of(timezone);
            return timezone;
        } catch (Exception ex) {
            return properties.getDefaultTimezone();
        }
    }

    private String serializeFilters(Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(filters);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid filters", e);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String buildUnsubscribeUrl(JobAlertSubscription subscription) {
        String base = properties.getUnsubscribeBaseUrl();
        if (base == null || base.isBlank()) {
            return "#";
        }
        String separator = base.contains("?") ? "&" : "?";
        return base + separator + "id=" + subscription.getId() + "&token=" + generateToken(subscription);
    }

    private String buildSummary(JobAlertSubscription subscription) {
        List<String> parts = new ArrayList<>();
        if (subscription.getSearchKeyword() != null) parts.add(subscription.getSearchKeyword());
        if (subscription.getCompany() != null) parts.add(subscription.getCompany());
        if (subscription.getLocation() != null) parts.add(subscription.getLocation());
        if (parts.isEmpty()) {
            return "全部职位";
        }
        return String.join(" · ", parts);
    }

    private String generateToken(JobAlertSubscription subscription) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getUnsubscribeSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String payload = subscription.getUserId() + ":" + subscription.getId();
            byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate token", ex);
        }
    }

    private boolean isValidToken(JobAlertSubscription subscription, String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String expected = generateToken(subscription);
        return Objects.equals(expected, token);
    }
}

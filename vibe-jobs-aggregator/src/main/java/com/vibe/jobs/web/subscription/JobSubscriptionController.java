package com.vibe.jobs.web.subscription;

import com.vibe.jobs.subscription.domain.JobAlertSubscription;
import com.vibe.jobs.subscription.service.CreateJobAlertCommand;
import com.vibe.jobs.subscription.service.JobAlertSubscriptionService;
import com.vibe.jobs.subscription.service.UpdateJobAlertCommand;
import com.vibe.jobs.web.session.UserPrincipal;
import com.vibe.jobs.web.subscription.dto.CreateSubscriptionRequest;
import com.vibe.jobs.web.subscription.dto.SubscriptionResponse;
import com.vibe.jobs.web.subscription.dto.UnsubscribeRequest;
import com.vibe.jobs.web.subscription.dto.UpdateSubscriptionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/subscriptions")
@CrossOrigin(origins = "*")
public class JobSubscriptionController {

    private final JobAlertSubscriptionService subscriptionService;

    public JobSubscriptionController(JobAlertSubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    public List<SubscriptionResponse> list(UserPrincipal principal) {
        return subscriptionService.listFor(principal.userId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponse create(UserPrincipal principal,
                                       @Valid @RequestBody CreateSubscriptionRequest request) {
        JobAlertSubscription subscription = subscriptionService.create(
                principal.userId(),
                principal.email(),
                new CreateJobAlertCommand(
                        request.keyword(),
                        request.company(),
                        request.location(),
                        request.level(),
                        request.filters(),
                        request.scheduleHour(),
                        request.timezone()
                )
        );
        return toResponse(subscription);
    }

    @PatchMapping("/{id}")
    public SubscriptionResponse update(UserPrincipal principal,
                                       @PathVariable Long id,
                                       @RequestBody UpdateSubscriptionRequest request) {
        JobAlertSubscription updated = subscriptionService.update(
                principal.userId(),
                id,
                new UpdateJobAlertCommand(
                        request.keyword(),
                        request.company(),
                        request.location(),
                        request.level(),
                        request.filters(),
                        request.scheduleHour(),
                        request.timezone(),
                        request.status()
                )
        );
        return toResponse(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(UserPrincipal principal, @PathVariable Long id) {
        subscriptionService.cancel(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<Void> triggerTest(UserPrincipal principal, @PathVariable Long id) {
        subscriptionService.triggerTest(principal.userId(), id);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@PathVariable Long id,
                                            @Valid @RequestBody UnsubscribeRequest request) {
        subscriptionService.unsubscribe(id, request.token());
        return ResponseEntity.noContent().build();
    }

    private SubscriptionResponse toResponse(JobAlertSubscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getSearchKeyword(),
                subscription.getCompany(),
                subscription.getLocation(),
                subscription.getLevel(),
                subscription.getFiltersJson(),
                subscription.getScheduleHour(),
                subscription.getTimezone(),
                subscription.getStatus(),
                subscription.getLastNotifiedAt(),
                subscription.getCreatedAt()
        );
    }
}

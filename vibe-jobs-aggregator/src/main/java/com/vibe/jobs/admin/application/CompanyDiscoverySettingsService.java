package com.vibe.jobs.admin.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.jobs.admin.domain.CompanyDiscoverySettingsSnapshot;
import com.vibe.jobs.admin.domain.event.CompanyDiscoverySettingsUpdatedEvent;
import com.vibe.jobs.admin.infrastructure.jpa.CompanyDiscoverySettingsEntity;
import com.vibe.jobs.admin.infrastructure.jpa.SpringDataCompanyDiscoverySettingsRepository;
import com.vibe.jobs.shared.infrastructure.config.CompanyDiscoveryProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class CompanyDiscoverySettingsService {

    private static final Logger log = LoggerFactory.getLogger(CompanyDiscoverySettingsService.class);

    private final SpringDataCompanyDiscoverySettingsRepository repository;
    private final ObjectMapper objectMapper;
    private final CompanyDiscoveryProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    private final AtomicReference<CompanyDiscoverySettingsSnapshot> current;

    public CompanyDiscoverySettingsService(SpringDataCompanyDiscoverySettingsRepository repository,
                                          ObjectMapper objectMapper,
                                          CompanyDiscoveryProperties properties,
                                          ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.eventPublisher = eventPublisher;
        this.current = new AtomicReference<>();
    }

    @PostConstruct
    public void onApplicationStarted() {
        initializeIfNeeded();
    }

    @Transactional(readOnly = true)
    public CompanyDiscoverySettingsSnapshot current() {
        CompanyDiscoverySettingsSnapshot snapshot = current.get();
        if (snapshot != null) {
            return snapshot;
        }
        return loadOrInitialize();
    }

    @Transactional
    public synchronized CompanyDiscoverySettingsSnapshot initializeIfNeeded() {
        CompanyDiscoverySettingsSnapshot snapshot = current.get();
        if (snapshot != null) {
            return snapshot;
        }
        snapshot = loadOrInitialize();
        current.set(snapshot);
        snapshot.applyTo(properties);
        return snapshot;
    }

    @Transactional
    public CompanyDiscoverySettingsSnapshot update(CompanyDiscoverySettingsSnapshot snapshot) {
        CompanyDiscoverySettingsSnapshot normalized = new CompanyDiscoverySettingsSnapshot(
                snapshot.enabled(),
                snapshot.fixedDelayMs(),
                snapshot.initialDelayMs(),
                snapshot.pageSize(),
                snapshot.maxCandidatesPerRun(),
                snapshot.dryRun(),
                snapshot.includeDataSourceTypes(),
                snapshot.excludeCompanies(),
                snapshot.providers(),
                snapshot.locationFilter(),
                snapshot.roleFilter(),
                Instant.now()
        );
        persist(normalized);
        normalized.applyTo(properties);
        current.set(normalized);
        eventPublisher.publishEvent(new CompanyDiscoverySettingsUpdatedEvent(normalized));
        return normalized;
    }

    private CompanyDiscoverySettingsSnapshot loadOrInitialize() {
        CompanyDiscoverySettingsSnapshot snapshot = repository.findBySettingKey("main")
                .map(this::toSnapshot)
                .map(existing -> {
                    existing.applyTo(properties);
                    return existing;
                })
                .orElseGet(() -> {
                    CompanyDiscoverySettingsSnapshot created = CompanyDiscoverySettingsSnapshot.fromProperties(properties, Instant.now());
                    persist(created);
                    return created;
                });
        current.set(snapshot);
        return snapshot;
    }

    private void persist(CompanyDiscoverySettingsSnapshot snapshot) {
        String json = serialize(snapshot);
        CompanyDiscoverySettingsEntity entity = repository.findBySettingKey("main")
                .orElse(new CompanyDiscoverySettingsEntity());
        entity.setSettingKey("main");
        entity.setSettingValue(json);
        repository.save(entity);
    }

    private CompanyDiscoverySettingsSnapshot toSnapshot(CompanyDiscoverySettingsEntity entity) {
        try {
            CompanyDiscoverySettingsSnapshot snapshot = objectMapper.readValue(entity.getSettingValue(), CompanyDiscoverySettingsSnapshot.class);
            return new CompanyDiscoverySettingsSnapshot(
                    snapshot.enabled(),
                    snapshot.fixedDelayMs(),
                    snapshot.initialDelayMs(),
                    snapshot.pageSize(),
                    snapshot.maxCandidatesPerRun(),
                    snapshot.dryRun(),
                    snapshot.includeDataSourceTypes(),
                    snapshot.excludeCompanies(),
                    snapshot.providers(),
                    snapshot.locationFilter(),
                    snapshot.roleFilter(),
                    entity.getUpdatedTime()
            );
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse company discovery settings JSON, falling back to defaults: {}", e.getMessage());
            return CompanyDiscoverySettingsSnapshot.fromProperties(properties, Instant.now());
        }
    }

    private String serialize(CompanyDiscoverySettingsSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize company discovery settings", e);
        }
    }
}

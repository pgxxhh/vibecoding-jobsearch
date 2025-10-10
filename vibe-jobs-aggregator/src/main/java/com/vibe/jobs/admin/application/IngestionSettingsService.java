package com.vibe.jobs.admin.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.jobs.admin.domain.IngestionSettingsSnapshot;
import com.vibe.jobs.admin.domain.event.IngestionSettingsUpdatedEvent;
import com.vibe.jobs.admin.infrastructure.jpa.IngestionSettingsEntity;
import com.vibe.jobs.admin.infrastructure.jpa.SpringDataIngestionSettingsRepository;
import com.vibe.jobs.config.IngestionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class IngestionSettingsService {

    private static final Logger log = LoggerFactory.getLogger(IngestionSettingsService.class);
    private static final long SETTINGS_ID = 1L;

    private final SpringDataIngestionSettingsRepository repository;
    private final ObjectMapper objectMapper;
    private final IngestionProperties ingestionProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final AtomicReference<IngestionSettingsSnapshot> current;

    public IngestionSettingsService(SpringDataIngestionSettingsRepository repository,
                                    ObjectMapper objectMapper,
                                    IngestionProperties ingestionProperties,
                                    ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.ingestionProperties = ingestionProperties;
        this.eventPublisher = eventPublisher;
        this.current = new AtomicReference<>();
    }

    @PostConstruct
    public void onApplicationStarted() {
        initializeIfNeeded();
    }

    @Transactional(readOnly = true)
    public IngestionSettingsSnapshot current() {
        IngestionSettingsSnapshot snapshot = current.get();
        if (snapshot != null) {
            return snapshot;
        }
        return loadOrInitialize();
    }

    @Transactional
    public synchronized IngestionSettingsSnapshot initializeIfNeeded() {
        IngestionSettingsSnapshot snapshot = current.get();
        if (snapshot != null) {
            return snapshot;
        }
        snapshot = loadOrInitialize();
        current.set(snapshot);
        snapshot.applyTo(ingestionProperties);
        return snapshot;
    }

    @Transactional
    public IngestionSettingsSnapshot update(IngestionSettingsSnapshot snapshot) {
        IngestionSettingsSnapshot normalized = new IngestionSettingsSnapshot(
                snapshot.fixedDelayMs(),
                snapshot.initialDelayMs(),
                snapshot.pageSize(),
                snapshot.mode(),
                snapshot.recentDays(),
                snapshot.concurrency(),
                snapshot.companyOverrides(),
                snapshot.locationFilter(),
                snapshot.roleFilter(),
                Instant.now()
        );
        persist(normalized);
        normalized.applyTo(ingestionProperties);
        current.set(normalized);
        eventPublisher.publishEvent(new IngestionSettingsUpdatedEvent(normalized));
        return normalized;
    }

    private IngestionSettingsSnapshot loadOrInitialize() {
        IngestionSettingsSnapshot snapshot = repository.findBySettingKey("main")
                .map(this::toSnapshot)
                .map(existingSnapshot -> {
                    existingSnapshot.applyTo(ingestionProperties);
                    return existingSnapshot;
                })
                .orElseGet(() -> {
                    IngestionSettingsSnapshot newSnapshot = IngestionSettingsSnapshot.fromProperties(ingestionProperties, Instant.now());
                    persist(newSnapshot);
                    return newSnapshot;
                });
        current.set(snapshot);
        return snapshot;
    }

    private void persist(IngestionSettingsSnapshot snapshot) {
        String json = serialize(snapshot);
        IngestionSettingsEntity entity = repository.findBySettingKey("main")
                .orElse(new IngestionSettingsEntity());
        entity.setSettingKey("main");
        entity.setSettingValue(json);
        repository.save(entity);
    }

    private IngestionSettingsSnapshot toSnapshot(IngestionSettingsEntity entity) {
        try {
            IngestionSettingsSnapshot snapshot = objectMapper.readValue(entity.getSettingValue(), IngestionSettingsSnapshot.class);
            return new IngestionSettingsSnapshot(
                    snapshot.fixedDelayMs(),
                    snapshot.initialDelayMs(),
                    snapshot.pageSize(),
                    snapshot.mode(),
                    snapshot.recentDays(),
                    snapshot.concurrency(),
                    snapshot.companyOverrides(),
                    snapshot.locationFilter(),
                    snapshot.roleFilter(),
                    entity.getUpdatedAt()
            );
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse ingestion settings JSON, falling back to defaults: {}", e.getMessage());
            return IngestionSettingsSnapshot.fromProperties(ingestionProperties, Instant.now());
        }
    }

    private String serialize(IngestionSettingsSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize ingestion settings", e);
        }
    }
}

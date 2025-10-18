package com.vibe.jobs.ingestion;

import com.vibe.jobs.ingestion.domain.IngestionCursor;
import com.vibe.jobs.ingestion.domain.IngestionCursorKey;
import com.vibe.jobs.ingestion.infrastructure.jpa.IngestionCursorEntity;
import com.vibe.jobs.ingestion.infrastructure.jpa.SpringDataIngestionCursorRepository;
import com.vibe.jobs.jobposting.domain.Job;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class IngestionCursorService {

    private final SpringDataIngestionCursorRepository repository;

    public IngestionCursorService(SpringDataIngestionCursorRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<IngestionCursor> find(IngestionCursorKey key) {
        IngestionCursorKey normalized = normalize(key);
        return repository.findBySourceNameAndCompanyAndCategory(
                        normalized.sourceName(),
                        normalized.company(),
                        normalized.category())
                .map(this::toDomain);
    }

    @Transactional
    public IngestionCursor updatePosition(IngestionCursorKey key, Job job) {
        if (job == null) {
            throw new IllegalArgumentException("job must not be null");
        }
        Instant postedAt = job.getPostedAt();
        String externalId = job.getExternalId();
        return updatePosition(key, postedAt, externalId, null);
    }

    @Transactional
    public IngestionCursor updatePosition(IngestionCursorKey key,
                                          Instant lastPostedAt,
                                          String lastExternalId,
                                          String nextPageToken) {
        IngestionCursorKey normalized = normalize(key);
        try {
            IngestionCursorEntity entity = repository
                    .findWithLockBySourceNameAndCompanyAndCategory(normalized.sourceName(), normalized.company(), normalized.category())
                    .orElseGet(() -> newEntity(normalized));
            applyState(entity, normalized, lastPostedAt, lastExternalId, nextPageToken);
            IngestionCursorEntity saved = repository.saveAndFlush(entity);
            return toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            IngestionCursorEntity entity = repository
                    .findWithLockBySourceNameAndCompanyAndCategory(normalized.sourceName(), normalized.company(), normalized.category())
                    .orElseThrow(() -> new IllegalStateException("Failed to load ingestion cursor after concurrent creation", ex));
            applyState(entity, normalized, lastPostedAt, lastExternalId, nextPageToken);
            IngestionCursorEntity saved = repository.saveAndFlush(entity);
            return toDomain(saved);
        }
    }

    private IngestionCursor toDomain(IngestionCursorEntity entity) {
        return new IngestionCursor(
                entity.getSourceCode(),
                entity.getSourceName(),
                entity.getCompany(),
                entity.getCategory(),
                entity.getLastPostedAt(),
                entity.getLastExternalId(),
                entity.getNextPageToken(),
                entity.getLastIngestedAt(),
                entity.getCreateTime(),
                entity.getUpdateTime()
        );
    }

    private IngestionCursorKey normalize(IngestionCursorKey key) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        return IngestionCursorKey.of(
                sanitize(key.sourceCode()),
                sanitize(key.sourceName()),
                sanitize(key.company()),
                sanitize(key.category())
        );
    }

    private IngestionCursorEntity newEntity(IngestionCursorKey normalized) {
        IngestionCursorEntity created = new IngestionCursorEntity();
        created.setSourceCode(normalized.sourceCode());
        created.setSourceName(normalized.sourceName());
        created.setCompany(normalized.company());
        created.setCategory(normalized.category());
        created.setLastIngestedAt(Instant.now());
        return created;
    }

    private void applyState(IngestionCursorEntity entity,
                            IngestionCursorKey normalized,
                            Instant lastPostedAt,
                            String lastExternalId,
                            String nextPageToken) {
        entity.setSourceCode(normalized.sourceCode());
        entity.setSourceName(normalized.sourceName());
        entity.setCompany(normalized.company());
        entity.setCategory(normalized.category());
        entity.setLastPostedAt(lastPostedAt);
        entity.setLastExternalId(lastExternalId);
        entity.setNextPageToken(nextPageToken);
        entity.setLastIngestedAt(Instant.now());
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}

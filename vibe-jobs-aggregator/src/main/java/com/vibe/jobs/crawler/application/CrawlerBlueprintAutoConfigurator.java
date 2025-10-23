package com.vibe.jobs.crawler.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vibe.jobs.crawler.domain.CrawlBlueprint;
import com.vibe.jobs.crawler.domain.CrawlerBlueprintDraft;
import com.vibe.jobs.crawler.domain.CrawlerBlueprintDraftRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.Optional;

@Service
public class CrawlerBlueprintAutoConfigurator {

    private static final Logger log = LoggerFactory.getLogger(CrawlerBlueprintAutoConfigurator.class);
    private static final String SYSTEM_OPERATOR = "auto-browser-guardian";
    private static final String METADATA_BROWSER_KEY = "autoBrowser";
    private static final String METADATA_REASON_KEY = "autoBrowserReason";
    private static final String METADATA_UPDATED_KEY = "autoBrowserUpdatedAt";

    private final CrawlerBlueprintDraftRepository draftRepository;
    private final ObjectMapper objectMapper;

    public CrawlerBlueprintAutoConfigurator(CrawlerBlueprintDraftRepository draftRepository,
                                            ObjectMapper objectMapper) {
        this.draftRepository = draftRepository;
        this.objectMapper = objectMapper;
    }

    public void handleHttpFailure(CrawlBlueprint blueprint, Throwable failure) {
        if (blueprint == null || failure == null) {
            return;
        }
        if (blueprint.requiresBrowser()) {
            return;
        }
        Optional<WebClientResponseException> httpFailure = findHttpException(failure);
        if (httpFailure.isEmpty()) {
            return;
        }
        WebClientResponseException exception = httpFailure.get();
        if (!exception.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
            return;
        }
        draftRepository.findByCode(blueprint.code()).ifPresentOrElse(
                draft -> applyBrowserRequirement(blueprint, draft, "HTTP_FORBIDDEN"),
                () -> log.info("Skipping auto browser promotion for {} - draft not found", blueprint.code())
        );
    }

    private Optional<WebClientResponseException> findHttpException(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof WebClientResponseException response) {
                return Optional.of(response);
            }
            current = current.getCause();
        }
        return Optional.empty();
    }

    private void applyBrowserRequirement(CrawlBlueprint blueprint,
                                         CrawlerBlueprintDraft draft,
                                         String reason) {
        buildUpdatedConfig(draft.configJson(), reason).ifPresent(updatedConfig -> {
            if (updatedConfig.equals(draft.configJson())) {
                return;
            }
            CrawlerBlueprintDraft updated = draft.requireBrowser(updatedConfig, SYSTEM_OPERATOR);
            draftRepository.save(updated);
            log.info("Blueprint {} promoted to browser engine automatically due to {}", blueprint.code(), reason);
        });
    }

    private Optional<String> buildUpdatedConfig(String rawConfig, String reason) {
        if (rawConfig == null || rawConfig.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode tree = objectMapper.readTree(rawConfig);
            if (!(tree instanceof ObjectNode objectNode)) {
                return Optional.empty();
            }
            ObjectNode automation = getOrCreateObject(objectNode, "automation");
            boolean alreadyBrowser = automation.path("jsEnabled").asBoolean(false);
            if (alreadyBrowser) {
                return Optional.empty();
            }
            automation.put("jsEnabled", true);
            if (!automation.has("enabled")) {
                automation.put("enabled", false);
            }
            ObjectNode metadata = getOrCreateObject(objectNode, "metadata");
            metadata.put(METADATA_BROWSER_KEY, true);
            metadata.put(METADATA_REASON_KEY, reason);
            metadata.put(METADATA_UPDATED_KEY, Instant.now().toString());
            String updatedJson = objectMapper.writeValueAsString(objectNode);
            return Optional.of(updatedJson);
        } catch (Exception ex) {
            log.warn("Failed to build updated config for auto browser promotion: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private ObjectNode getOrCreateObject(ObjectNode parent, String fieldName) {
        JsonNode existing = parent.get(fieldName);
        if (existing instanceof ObjectNode objectNode) {
            return objectNode;
        }
        return parent.putObject(fieldName);
    }
}

package com.vibe.jobs.admin.web;

import com.vibe.jobs.admin.application.AdminChangeLogService;
import com.vibe.jobs.admin.application.IngestionSettingsService;
import com.vibe.jobs.admin.domain.AdminPrincipal;
import com.vibe.jobs.admin.domain.IngestionSettingsSnapshot;
import com.vibe.jobs.admin.web.dto.IngestionSettingsRequest;
import com.vibe.jobs.admin.web.dto.IngestionSettingsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(path = "/admin/ingestion-settings", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminIngestionSettingsController {

    private static final Logger log = LoggerFactory.getLogger(AdminIngestionSettingsController.class);

    private final IngestionSettingsService ingestionSettingsService;
    private final AdminChangeLogService changeLogService;

    public AdminIngestionSettingsController(IngestionSettingsService ingestionSettingsService,
                                            AdminChangeLogService changeLogService) {
        this.ingestionSettingsService = ingestionSettingsService;
        this.changeLogService = changeLogService;
    }

    @GetMapping
    public ResponseEntity<IngestionSettingsResponse> getCurrent() {
        try {
            log.info("Getting current ingestion settings");
            IngestionSettingsSnapshot snapshot = ingestionSettingsService.current();
            log.info("Retrieved ingestion settings: recentDays={}, concurrency={}", snapshot.recentDays(), snapshot.concurrency());
            IngestionSettingsResponse response = IngestionSettingsResponse.fromSnapshot(snapshot);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting current ingestion settings", e);
            throw e;
        }
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IngestionSettingsResponse> update(@RequestBody IngestionSettingsRequest request,
                                                           AdminPrincipal principal) {
        try {
            log.info("Updating ingestion settings with request: {}", request);
            IngestionSettingsSnapshot before = ingestionSettingsService.current();
            IngestionSettingsSnapshot updated = ingestionSettingsService.update(request.toSnapshot());
            
            changeLogService.record(
                    principal != null ? principal.email() : null,
                    "UPDATE",
                    "INGESTION_SETTINGS",
                    "global",
                    Map.of("before", before, "after", updated)
            );
            
            IngestionSettingsResponse response = IngestionSettingsResponse.fromSnapshot(updated);
            log.info("Successfully updated ingestion settings");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating ingestion settings", e);
            throw e;
        }
    }
}

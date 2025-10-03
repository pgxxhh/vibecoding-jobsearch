package com.vibe.jobs.admin.web;

import com.vibe.jobs.admin.application.AdminChangeLogService;
import com.vibe.jobs.admin.application.IngestionSettingsService;
import com.vibe.jobs.admin.domain.AdminPrincipal;
import com.vibe.jobs.admin.domain.IngestionSettingsSnapshot;
import com.vibe.jobs.admin.web.dto.IngestionSettingsRequest;
import com.vibe.jobs.admin.web.dto.IngestionSettingsResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(path = "/admin/ingestion-settings", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminIngestionSettingsController {

    private final IngestionSettingsService ingestionSettingsService;
    private final AdminChangeLogService changeLogService;

    public AdminIngestionSettingsController(IngestionSettingsService ingestionSettingsService,
                                            AdminChangeLogService changeLogService) {
        this.ingestionSettingsService = ingestionSettingsService;
        this.changeLogService = changeLogService;
    }

    @GetMapping
    public IngestionSettingsResponse getCurrent() {
        IngestionSettingsSnapshot snapshot = ingestionSettingsService.current();
        return IngestionSettingsResponse.fromSnapshot(snapshot);
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public IngestionSettingsResponse update(@RequestBody IngestionSettingsRequest request,
                                            AdminPrincipal principal) {
        IngestionSettingsSnapshot before = ingestionSettingsService.current();
        IngestionSettingsSnapshot updated = ingestionSettingsService.update(request.toSnapshot());
        changeLogService.record(
                principal != null ? principal.email() : null,
                "UPDATE",
                "INGESTION_SETTINGS",
                "global",
                Map.of("before", before, "after", updated)
        );
        return IngestionSettingsResponse.fromSnapshot(updated);
    }
}

package com.vibe.jobs.admin.interfaces;

import com.vibe.jobs.admin.application.AdminChangeLogService;
import com.vibe.jobs.admin.application.CompanyDiscoverySettingsService;
import com.vibe.jobs.admin.domain.AdminPrincipal;
import com.vibe.jobs.admin.domain.CompanyDiscoverySettingsSnapshot;
import com.vibe.jobs.admin.interfaces.dto.CompanyDiscoveryResultResponse;
import com.vibe.jobs.admin.interfaces.dto.CompanyDiscoveryRunResponse;
import com.vibe.jobs.admin.interfaces.dto.CompanyDiscoverySettingsRequest;
import com.vibe.jobs.admin.interfaces.dto.CompanyDiscoverySettingsResponse;
import com.vibe.jobs.companydiscovery.CompanyDiscoveryScheduler;
import com.vibe.jobs.companydiscovery.application.CompanyDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/admin/company-discovery", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminCompanyDiscoveryController {

    private static final Logger log = LoggerFactory.getLogger(AdminCompanyDiscoveryController.class);

    private final CompanyDiscoverySettingsService settingsService;
    private final CompanyDiscoveryService discoveryService;
    private final CompanyDiscoveryScheduler scheduler;
    private final AdminChangeLogService changeLogService;

    public AdminCompanyDiscoveryController(CompanyDiscoverySettingsService settingsService,
                                           CompanyDiscoveryService discoveryService,
                                           CompanyDiscoveryScheduler scheduler,
                                           AdminChangeLogService changeLogService) {
        this.settingsService = settingsService;
        this.discoveryService = discoveryService;
        this.scheduler = scheduler;
        this.changeLogService = changeLogService;
    }

    @GetMapping("/settings")
    public ResponseEntity<CompanyDiscoverySettingsResponse> getSettings() {
        CompanyDiscoverySettingsSnapshot snapshot = settingsService.current();
        return ResponseEntity.ok(CompanyDiscoverySettingsResponse.fromSnapshot(snapshot));
    }

    @PutMapping(path = "/settings", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CompanyDiscoverySettingsResponse> updateSettings(@RequestBody CompanyDiscoverySettingsRequest request,
                                                                           AdminPrincipal principal) {
        CompanyDiscoverySettingsSnapshot before = settingsService.current();
        CompanyDiscoverySettingsSnapshot updated = settingsService.update(request.toSnapshot());
        changeLogService.record(
                principal != null ? principal.email() : null,
                "UPDATE",
                "COMPANY_DISCOVERY_SETTINGS",
                "global",
                Map.of("before", before, "after", updated)
        );
        return ResponseEntity.ok(CompanyDiscoverySettingsResponse.fromSnapshot(updated));
    }

    @PostMapping("/run")
    public ResponseEntity<CompanyDiscoveryRunResponse> triggerRun(AdminPrincipal principal) {
        log.info("Triggering company discovery run by {}", principal != null ? principal.email() : "system");
        scheduler.triggerImmediateRun(principal != null ? principal.email() : null);
        return ResponseEntity.accepted().body(new CompanyDiscoveryRunResponse(
                null,
                "PENDING",
                "manual",
                settingsService.current().dryRun(),
                0,
                0,
                null,
                null
        ));
    }

    @GetMapping("/runs")
    public ResponseEntity<List<CompanyDiscoveryRunResponse>> listRuns() {
        List<CompanyDiscoveryRunResponse> runs = discoveryService.listRuns(50).stream()
                .map(CompanyDiscoveryRunResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(runs);
    }

    @GetMapping("/results")
    public ResponseEntity<List<CompanyDiscoveryResultResponse>> listResults() {
        List<CompanyDiscoveryResultResponse> results = discoveryService.listResults(100).stream()
                .map(CompanyDiscoveryResultResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(results);
    }
}

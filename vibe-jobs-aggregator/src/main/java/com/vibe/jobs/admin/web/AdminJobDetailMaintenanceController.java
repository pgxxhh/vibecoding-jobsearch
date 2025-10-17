package com.vibe.jobs.admin.web;

import com.vibe.jobs.jobposting.application.JobDetailMaintenanceService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/admin/job-details", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminJobDetailMaintenanceController {

    private final JobDetailMaintenanceService jobDetailMaintenanceService;

    public AdminJobDetailMaintenanceController(JobDetailMaintenanceService jobDetailMaintenanceService) {
        this.jobDetailMaintenanceService = jobDetailMaintenanceService;
    }

    @PostMapping("/normalize-content-text")
    public MaintenanceResponse normalizeContentText(@RequestParam(name = "batchSize", required = false) Integer batchSize) {
        var result = jobDetailMaintenanceService.normalizeContentText(batchSize);
        return new MaintenanceResponse(result.processed(), result.updated(), result.batches(), result.batchSize());
    }

    public record MaintenanceResponse(long processed, long updated, int batches, int batchSize) {
    }
}

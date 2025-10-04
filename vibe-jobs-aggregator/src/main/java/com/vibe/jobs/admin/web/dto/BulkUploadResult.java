package com.vibe.jobs.admin.web.dto;

import java.util.List;

public record BulkUploadResult(
        int success,
        int failed,
        List<String> errors
) {
}
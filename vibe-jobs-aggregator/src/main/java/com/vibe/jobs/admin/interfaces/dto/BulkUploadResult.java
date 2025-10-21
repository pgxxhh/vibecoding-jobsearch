package com.vibe.jobs.admin.interfaces.dto;

import java.util.List;

public record BulkUploadResult(
        int success,
        int failed,
        List<String> errors
) {
}
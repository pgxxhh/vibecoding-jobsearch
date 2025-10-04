package com.vibe.jobs.admin.web.dto;

import java.util.List;

public record BulkCompanyRequest(List<CompanyRequest> companies) {
    
    public BulkCompanyRequest {
        if (companies == null) {
            companies = List.of();
        }
    }
}
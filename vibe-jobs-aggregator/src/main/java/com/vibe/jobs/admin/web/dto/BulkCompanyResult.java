package com.vibe.jobs.admin.web.dto;

import java.util.List;

public record BulkCompanyResult(
    int total,
    int successful,
    int failed,
    List<CompanyResponse> created,
    List<String> errors
) {
    
    public static BulkCompanyResult success(List<CompanyResponse> created) {
        return new BulkCompanyResult(
            created.size(),
            created.size(), 
            0,
            created,
            List.of()
        );
    }
    
    public static BulkCompanyResult withErrors(
        List<CompanyResponse> created, 
        List<String> errors
    ) {
        int total = created.size() + errors.size();
        return new BulkCompanyResult(
            total,
            created.size(),
            errors.size(),
            created,
            errors
        );
    }
}
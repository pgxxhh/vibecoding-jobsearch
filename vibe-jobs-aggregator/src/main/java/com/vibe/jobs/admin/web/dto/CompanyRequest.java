package com.vibe.jobs.admin.web.dto;

import com.vibe.jobs.datasource.domain.JobDataSource.DataSourceCompany;

import java.util.Map;

public record CompanyRequest(
        Long id,
        String reference,
        String displayName,
        String slug,
        Boolean enabled,
        Map<String, String> placeholderOverrides,
        Map<String, String> overrideOptions
) {
    public DataSourceCompany toDomain() {
        return new DataSourceCompany(
                id,
                reference != null ? reference : "",
                displayName,
                slug,
                enabled != null ? enabled : true,
                placeholderOverrides,
                overrideOptions
        );
    }
}

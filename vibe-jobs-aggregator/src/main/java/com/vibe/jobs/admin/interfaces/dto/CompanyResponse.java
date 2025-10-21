package com.vibe.jobs.admin.interfaces.dto;

import com.vibe.jobs.datasource.domain.JobDataSource.DataSourceCompany;

import java.util.Map;

public record CompanyResponse(
        Long id,
        String reference,
        String displayName,
        String slug,
        boolean enabled,
        Map<String, String> placeholderOverrides,
        Map<String, String> overrideOptions
) {
    public static CompanyResponse fromDomain(DataSourceCompany company) {
        return new CompanyResponse(
                company.id(),
                company.reference(),
                company.displayName(),
                company.slug(),
                company.enabled(),
                company.placeholderOverrides(),
                company.overrideOptions()
        );
    }
}
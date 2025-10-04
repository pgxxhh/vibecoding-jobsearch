package com.vibe.jobs.admin.web.dto;

import com.vibe.jobs.datasource.domain.JobDataSource;

import java.util.List;
import java.util.Map;

public record PagedDataSourceResponse(
        Long id,
        String code,
        String type,
        boolean enabled,
        boolean runOnStartup,
        boolean requireOverride,
        JobDataSource.Flow flow,
        Map<String, String> baseOptions,
        List<JobDataSource.CategoryQuotaDefinition> categories,
        PagedCompanyResponse companies
) {
    public static PagedDataSourceResponse fromDomain(JobDataSource source, PagedCompanyResponse companies) {
        return new PagedDataSourceResponse(
                source.getId(),
                source.getCode(),
                source.getType(),
                source.isEnabled(),
                source.isRunOnStartup(),
                source.isRequireOverride(),
                source.getFlow(),
                source.getBaseOptions(),
                source.getCategories(),
                companies
        );
    }

    public record PagedCompanyResponse(
            List<JobDataSource.DataSourceCompany> content,
            int page,
            int size,
            int totalPages,
            long totalElements,
            boolean hasNext,
            boolean hasPrevious
    ) {}
}
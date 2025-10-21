package com.vibe.jobs.admin.interfaces.dto;

import com.vibe.jobs.datasource.domain.JobDataSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DataSourceRequest(
        String code,
        String type,
        boolean enabled,
        boolean runOnStartup,
        boolean requireOverride,
        String flow,
        Map<String, String> baseOptions,
        List<CategoryRequest> categories,
        List<CompanyRequest> companies
) {
    public JobDataSource toDomain(Long id) {
        JobDataSource.Flow resolvedFlow;
        try {
            resolvedFlow = flow == null ? JobDataSource.Flow.UNLIMITED : JobDataSource.Flow.valueOf(flow.toUpperCase());
        } catch (IllegalArgumentException ex) {
            resolvedFlow = JobDataSource.Flow.UNLIMITED;
        }
        Map<String, String> options = baseOptions == null ? Map.of() : new LinkedHashMap<>(baseOptions);
        List<JobDataSource.CategoryQuotaDefinition> quotaDefinitions = new ArrayList<>();
        if (categories != null) {
            for (CategoryRequest category : categories) {
                quotaDefinitions.add(category.toDomain());
            }
        }
        List<JobDataSource.DataSourceCompany> companyList = new ArrayList<>();
        if (companies != null) {
            for (CompanyRequest company : companies) {
                companyList.add(company.toDomain());
            }
        }
        return new JobDataSource(
                id,
                code,
                type,
                enabled,
                runOnStartup,
                requireOverride,
                resolvedFlow,
                options,
                quotaDefinitions,
                companyList
        );
    }

    public record CategoryRequest(
            String name,
            int limit,
            List<String> tags,
            Map<String, List<String>> facets
    ) {
        public JobDataSource.CategoryQuotaDefinition toDomain() {
            List<String> normalizedTags = tags == null ? List.of() : new ArrayList<>(tags);
            Map<String, List<String>> normalizedFacets = new LinkedHashMap<>();
            if (facets != null) {
                facets.forEach((key, values) -> {
                    if (key == null) {
                        return;
                    }
                    normalizedFacets.put(key, values == null ? List.of() : new ArrayList<>(values));
                });
            }
            return new JobDataSource.CategoryQuotaDefinition(
                    name,
                    limit,
                    normalizedTags,
                    normalizedFacets
            );
        }
    }

    public record CompanyRequest(
            Long id,
            String reference,
            String displayName,
            String slug,
            boolean enabled,
            Map<String, String> placeholderOverrides,
            Map<String, String> overrideOptions
    ) {
        public JobDataSource.DataSourceCompany toDomain() {
            Map<String, String> placeholders = placeholderOverrides == null ? Map.of() : new LinkedHashMap<>(placeholderOverrides);
            Map<String, String> overrides = overrideOptions == null ? Map.of() : new LinkedHashMap<>(overrideOptions);
            return new JobDataSource.DataSourceCompany(id, reference, displayName, slug, enabled, placeholders, overrides);
        }
    }
}

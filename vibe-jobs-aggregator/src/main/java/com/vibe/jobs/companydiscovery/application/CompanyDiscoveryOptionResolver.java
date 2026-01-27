package com.vibe.jobs.companydiscovery.application;

import com.vibe.jobs.datasource.domain.JobDataSource;
import com.vibe.jobs.datasource.domain.PlaceholderContext;
import com.vibe.jobs.datasource.domain.SourceOptionDefaults;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CompanyDiscoveryOptionResolver {

    public Map<String, String> resolve(JobDataSource source, JobDataSource.DataSourceCompany company) {
        PlaceholderContext context = PlaceholderContext.forCompany(company);
        Map<String, String> merged = new LinkedHashMap<>(source.getBaseOptions());
        merged.replaceAll((k, v) -> applyPlaceholders(v, context));

        Map<String, String> defaults = SourceOptionDefaults.derive(source.getType(), context);
        defaults.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                merged.putIfAbsent(key, value);
            }
        });

        Map<String, String> overrideOptions = company == null ? Map.of() : company.overrideOptions();
        overrideOptions.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                merged.put(key, context.apply(value));
            }
        });

        if (!context.company().isBlank()) {
            merged.putIfAbsent("company", context.company());
        }
        if (source.getCode() != null) {
            merged.putIfAbsent("__sourceCode", source.getCode());
            merged.putIfAbsent("__sourceName", "crawler:" + source.getCode());
        }
        if (!context.company().isBlank()) {
            merged.put("__company", context.company());
        }

        merged.replaceAll((k, v) -> applyPlaceholders(v, context));
        merged.values().removeIf(value -> value == null || value.isBlank());
        return merged;
    }

    private String applyPlaceholders(String value, PlaceholderContext context) {
        return value == null ? null : context.apply(value);
    }
}

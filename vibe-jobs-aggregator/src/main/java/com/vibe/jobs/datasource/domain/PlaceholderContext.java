package com.vibe.jobs.datasource.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public record PlaceholderContext(String company,
                                 String companyLower,
                                 String companyUpper,
                                 String slug,
                                 String slugUpper,
                                 Map<String, String> custom) {

    public PlaceholderContext {
        company = sanitize(company);
        companyLower = sanitize(companyLower);
        companyUpper = sanitize(companyUpper);
        slug = sanitize(slug);
        slugUpper = sanitize(slugUpper);
        custom = custom == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(custom));
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.trim();
    }

    public PlaceholderContext withCustom(String key, String value) {
        if (key == null || key.isBlank()) {
            return this;
        }
        Map<String, String> updated = new LinkedHashMap<>(custom);
        if (value == null) {
            updated.remove(key);
        } else {
            updated.put(key.trim(), value);
        }
        return new PlaceholderContext(company, companyLower, companyUpper, slug, slugUpper, updated);
    }

    public String apply(String value) {
        if (value == null) {
            return null;
        }
        String replaced = value
                .replace("{{company}}", company)
                .replace("{{companyLower}}", companyLower)
                .replace("{{companyUpper}}", companyUpper)
                .replace("{{slug}}", slug)
                .replace("{{slugUpper}}", slugUpper);
        for (Map.Entry<String, String> entry : custom.entrySet()) {
            replaced = replaced.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return replaced;
    }

    public static PlaceholderContext forCompany(JobDataSource.DataSourceCompany company) {
        String reference = company == null ? "" : company.reference();
        String displayName = company == null ? "" : company.displayName();
        String slug = company == null ? "" : company.slug();

        String normalizedCompany = displayName.isBlank() ? reference : displayName;
        if (normalizedCompany.isBlank()) {
            normalizedCompany = reference;
        }
        normalizedCompany = normalizedCompany == null ? "" : normalizedCompany.trim();
        if (slug.isBlank()) {
            slug = slugify(normalizedCompany.isBlank() ? reference : normalizedCompany);
        }
        String lower = normalizedCompany.toLowerCase(Locale.ROOT);
        String upper = normalizedCompany.toUpperCase(Locale.ROOT);
        String slugUpper = slug.toUpperCase(Locale.ROOT);

        Map<String, String> overrides = new LinkedHashMap<>(company == null ? Map.of() : company.placeholderOverrides());
        String overrideCompany = overrides.remove("company");
        if (overrideCompany != null && !overrideCompany.isBlank()) {
            normalizedCompany = overrideCompany.trim();
            lower = normalizedCompany.toLowerCase(Locale.ROOT);
            upper = normalizedCompany.toUpperCase(Locale.ROOT);
        }
        String overrideSlug = overrides.remove("slug");
        if (overrideSlug != null && !overrideSlug.isBlank()) {
            slug = overrideSlug.trim();
        }
        String overrideSlugUpper = overrides.remove("slugUpper");
        if (overrideSlugUpper != null && !overrideSlugUpper.isBlank()) {
            slugUpper = overrideSlugUpper.trim();
        } else {
            slugUpper = slug.toUpperCase(Locale.ROOT);
        }
        String overrideLower = overrides.remove("companyLower");
        if (overrideLower != null && !overrideLower.isBlank()) {
            lower = overrideLower.trim();
        }
        String overrideUpper = overrides.remove("companyUpper");
        if (overrideUpper != null && !overrideUpper.isBlank()) {
            upper = overrideUpper.trim();
        }

        return new PlaceholderContext(
                normalizedCompany,
                lower,
                upper,
                slug,
                slugUpper,
                overrides
        );
    }

    private static String slugify(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}

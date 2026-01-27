package com.vibe.jobs.companydiscovery.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record CompanyCandidate(String reference,
                               String displayName,
                               String slug,
                               Map<String, String> placeholderOverrides,
                               Map<String, String> overrideOptions,
                               String provider) {

    public CompanyCandidate {
        reference = sanitize(reference);
        displayName = sanitize(displayName);
        slug = sanitize(slug);
        placeholderOverrides = placeholderOverrides == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(placeholderOverrides));
        overrideOptions = overrideOptions == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(overrideOptions));
        provider = sanitize(provider);
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}

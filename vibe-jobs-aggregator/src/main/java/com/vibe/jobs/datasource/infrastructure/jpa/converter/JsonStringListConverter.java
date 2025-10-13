package com.vibe.jobs.datasource.infrastructure.jpa.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;

@Converter(autoApply = true)
public class JsonStringListConverter extends AbstractJsonAttributeConverter<List<String>> {

    public JsonStringListConverter() {
        super(new TypeReference<>() {});
    }

    @Override
    protected List<String> convertNonJsonValue(String dbData) {
        String trimmed = dbData == null ? null : dbData.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            return null;
        }
        List<String> values = Arrays.stream(trimmed.split(","))
                .map(String::trim)
                .map(JsonStringListConverter::stripEnclosingQuotes)
                .filter(value -> !value.isEmpty())
                .toList();
        return values.isEmpty() ? null : values;
    }

    private static String stripEnclosingQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if (first == last && (first == '"' || first == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}

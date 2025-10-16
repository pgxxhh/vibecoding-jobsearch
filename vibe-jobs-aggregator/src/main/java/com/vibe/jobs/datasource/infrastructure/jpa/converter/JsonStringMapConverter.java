package com.vibe.jobs.datasource.infrastructure.jpa.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Converter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Converter(autoApply = true)
public class JsonStringMapConverter extends AbstractJsonAttributeConverter<Map<String, String>> {

    public JsonStringMapConverter() {
        super(new TypeReference<>() {});
    }

    @Override
    protected Map<String, String> convertNonJsonValue(String dbData) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> raw = mapper.readValue(dbData, new TypeReference<Map<String, Object>>() {});
            Map<String, String> normalised = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                Object value = entry.getValue();
                if (value == null) {
                    continue;
                }
                String text;
                if (value instanceof String str) {
                    text = str;
                } else if (value instanceof Iterable<?> iterable) {
                    text = toJoinedString(iterable);
                } else if (value.getClass().isArray()) {
                    text = toJoinedStringFromArray(value);
                } else {
                    text = mapper.writeValueAsString(value);
                }
                normalised.put(entry.getKey(), text);
            }
            return normalised;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String toJoinedString(Iterable<?> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .map(String::trim)
                .filter(str -> !str.isEmpty())
                .collect(Collectors.joining(","));
    }

    private static String toJoinedString(Object[] array) {
        return java.util.Arrays.stream(array)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .map(String::trim)
                .filter(str -> !str.isEmpty())
                .collect(Collectors.joining(","));
    }

    private static String toJoinedStringFromArray(Object array) {
        if (array instanceof Object[] obj) {
            return toJoinedString(obj);
        }
        int length = java.lang.reflect.Array.getLength(array);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            Object element = java.lang.reflect.Array.get(array, i);
            if (element == null) {
                continue;
            }
            String text = element.toString().trim();
            if (text.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(',');
            }
            builder.append(text);
        }
        return builder.toString();
    }
}

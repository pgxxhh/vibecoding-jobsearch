package com.vibe.jobs.datasource.infrastructure.jpa.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;

import java.util.Map;

@Converter(autoApply = true)
public class JsonStringMapConverter extends AbstractJsonAttributeConverter<Map<String, String>> {

    public JsonStringMapConverter() {
        super(new TypeReference<>() {});
    }
}

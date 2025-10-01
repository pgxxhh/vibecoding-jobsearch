package com.vibe.jobs.datasource.infrastructure.jpa.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;

import java.util.List;
import java.util.Map;

@Converter(autoApply = true)
public class JsonStringListMapConverter extends AbstractJsonAttributeConverter<Map<String, List<String>>> {

    public JsonStringListMapConverter() {
        super(new TypeReference<>() {});
    }
}

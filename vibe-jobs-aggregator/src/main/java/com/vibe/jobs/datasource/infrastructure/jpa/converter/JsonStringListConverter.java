package com.vibe.jobs.datasource.infrastructure.jpa.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;

import java.util.List;

@Converter(autoApply = true)
public class JsonStringListConverter extends AbstractJsonAttributeConverter<List<String>> {

    public JsonStringListConverter() {
        super(new TypeReference<>() {});
    }
}

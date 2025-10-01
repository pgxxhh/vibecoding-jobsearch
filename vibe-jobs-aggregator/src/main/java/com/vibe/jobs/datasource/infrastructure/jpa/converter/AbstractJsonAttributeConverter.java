package com.vibe.jobs.datasource.infrastructure.jpa.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Converter
public abstract class AbstractJsonAttributeConverter<T> implements AttributeConverter<T, String> {

    private static final Logger log = LoggerFactory.getLogger(AbstractJsonAttributeConverter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final TypeReference<T> typeReference;

    protected AbstractJsonAttributeConverter(TypeReference<T> typeReference) {
        this.typeReference = typeReference;
    }

    @Override
    public String convertToDatabaseColumn(T attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize attribute {}", attribute, e);
            return null;
        }
    }

    @Override
    public T convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, typeReference);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize attribute {}", dbData, e);
            return null;
        }
    }
}

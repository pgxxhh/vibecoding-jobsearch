package com.vibe.jobs.datasource.infrastructure.jpa.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON转换器，支持混合数据类型的Map转换
 * 可以处理String、Array等不同类型的值，将它们统一转换为String类型存储在Map中
 */
@Converter
public class JsonMixedTypeMapConverter implements AttributeConverter<Map<String, String>, String> {

    private static final Logger log = LoggerFactory.getLogger(JsonMixedTypeMapConverter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
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
    public Map<String, String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        
        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(dbData);
            Map<String, String> result = new LinkedHashMap<>();
            
            // 遍历JSON节点，将所有类型的值转换为String
            rootNode.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode valueNode = entry.getValue();
                
                String stringValue;
                if (valueNode.isTextual()) {
                    stringValue = valueNode.asText();
                } else if (valueNode.isArray()) {
                    // 将数组转换为逗号分隔的字符串
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < valueNode.size(); i++) {
                        if (i > 0) sb.append(",");
                        sb.append(valueNode.get(i).asText());
                    }
                    stringValue = sb.toString();
                } else if (valueNode.isObject()) {
                    // 将对象转换为JSON字符串
                    try {
                        stringValue = OBJECT_MAPPER.writeValueAsString(valueNode);
                    } catch (JsonProcessingException e) {
                        log.warn("Failed to serialize object value for key {}: {}", key, valueNode, e);
                        stringValue = valueNode.toString();
                    }
                } else {
                    // 其他类型（数字、布尔值等）直接转换为字符串
                    stringValue = valueNode.asText();
                }
                
                result.put(key, stringValue);
            });
            
            return result;
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize mixed-type JSON attribute {}, attempting fallback to Map<String, String>", dbData, e);
            
            // 回退方案：尝试直接反序列化为Map<String, String>
            try {
                return OBJECT_MAPPER.readValue(dbData, new TypeReference<Map<String, String>>() {});
            } catch (JsonProcessingException fallbackException) {
                log.error("Both primary and fallback deserialization failed for attribute: {}", dbData, fallbackException);
                return new LinkedHashMap<>();
            }
        }
    }
}
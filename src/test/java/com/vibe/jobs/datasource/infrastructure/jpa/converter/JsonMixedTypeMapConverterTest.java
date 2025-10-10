package com.vibe.jobs.datasource.infrastructure.jpa.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JsonMixedTypeMapConverterTest {

    private JsonMixedTypeMapConverter converter;

    @BeforeEach
    void setUp() {
        converter = new JsonMixedTypeMapConverter();
    }

    @Test
    void testConvertMixedTypeJson() {
        // This is the exact JSON structure from the error log
        String json = "{\"searchQuery\":\"software engineer OR data engineer OR financial analyst\",\"locations\":[\"Shanghai, China\",\"Beijing, China\",\"Singapore\",\"Hong Kong\"]}";
        
        Map<String, String> result = converter.convertToEntityAttribute(json);
        
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("software engineer OR data engineer OR financial analyst", result.get("searchQuery"));
        assertEquals("Shanghai, China,Beijing, China,Singapore,Hong Kong", result.get("locations"));
    }
}

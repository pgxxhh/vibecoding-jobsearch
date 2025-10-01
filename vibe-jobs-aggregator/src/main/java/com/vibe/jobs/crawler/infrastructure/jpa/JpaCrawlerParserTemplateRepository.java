package com.vibe.jobs.crawler.infrastructure.jpa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.jobs.crawler.domain.CrawlerParserTemplateRepository;
import com.vibe.jobs.crawler.domain.ParserField;
import com.vibe.jobs.crawler.domain.ParserFieldType;
import com.vibe.jobs.crawler.domain.ParserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public class JpaCrawlerParserTemplateRepository implements CrawlerParserTemplateRepository {

    private static final Logger log = LoggerFactory.getLogger(JpaCrawlerParserTemplateRepository.class);

    private final SpringDataCrawlerParserTemplateJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public JpaCrawlerParserTemplateRepository(SpringDataCrawlerParserTemplateJpaRepository jpaRepository,
                                              ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<ParserProfile> findByCode(String code) {
        return jpaRepository.findById(code == null ? "" : code.trim())
                .map(entity -> parseProfile(entity.getConfigJson()));
    }

    private ParserProfile parseProfile(String json) {
        if (json == null || json.isBlank()) {
            return ParserProfile.empty();
        }
        try {
            ParserConfig config = objectMapper.readValue(json, ParserConfig.class);
            Map<String, ParserField> fields = new LinkedHashMap<>();
            if (config.fields != null) {
                config.fields.forEach((name, field) -> {
                    ParserFieldType type = ParserFieldType.TEXT;
                    if (field.type != null) {
                        try {
                            type = ParserFieldType.valueOf(field.type.trim().toUpperCase(Locale.ROOT));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                    fields.put(name, new ParserField(
                            name,
                            type,
                            field.selector,
                            field.attribute,
                            field.constant,
                            field.format,
                            field.delimiter,
                            Boolean.TRUE.equals(field.required)
                    ));
                });
            }
            Set<String> tagFields = config.tagFields == null ? Set.of() : new LinkedHashSet<>(config.tagFields);
            return ParserProfile.of(config.listSelector, fields, tagFields, config.descriptionField);
        } catch (IOException e) {
            log.warn("Failed to parse crawler parser template: {}", e.getMessage());
            return ParserProfile.empty();
        }
    }

    static class ParserConfig {
        public String listSelector;
        public Map<String, Field> fields;
        public Set<String> tagFields;
        public String descriptionField;

        static class Field {
            public String type;
            public String selector;
            public String attribute;
            public String constant;
            public String format;
            public String delimiter;
            public Boolean required;
        }
    }
}

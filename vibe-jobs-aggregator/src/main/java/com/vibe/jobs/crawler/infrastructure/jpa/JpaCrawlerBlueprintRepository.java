package com.vibe.jobs.crawler.infrastructure.jpa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.jobs.crawler.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.*;

@Repository
public class JpaCrawlerBlueprintRepository implements CrawlerBlueprintRepository {

    private static final Logger log = LoggerFactory.getLogger(JpaCrawlerBlueprintRepository.class);

    private final SpringDataCrawlerBlueprintJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;
    private final CrawlerParserTemplateRepository templateRepository;

    public JpaCrawlerBlueprintRepository(SpringDataCrawlerBlueprintJpaRepository jpaRepository,
                                         ObjectMapper objectMapper,
                                         CrawlerParserTemplateRepository templateRepository) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
        this.templateRepository = templateRepository;
    }

    @Override
    public Optional<CrawlBlueprint> findByCode(String code) {
        return jpaRepository.findById(code == null ? "" : code.trim())
                .map(this::mapEntity);
    }

    @Override
    public List<CrawlBlueprint> findAllEnabled() {
        return jpaRepository.findAllByEnabledTrue().stream()
                .map(this::mapEntity)
                .filter(Objects::nonNull)
                .toList();
    }

    private CrawlBlueprint mapEntity(CrawlerBlueprintEntity entity) {
        if (entity == null) {
            return null;
        }
        BlueprintConfig config = readConfig(entity.getConfigJson());
        ParserProfile parserProfile = resolveParser(entity, config);
        PagingStrategy pagingStrategy = resolvePaging(config);
        CrawlFlow flow = resolveFlow(config);
        CrawlBlueprint.RateLimit rateLimit = resolveRateLimit(config);
        Map<String, Object> metadata = config == null || config.metadata == null ? Map.of() : config.metadata;
        String entryUrl = entity.getEntryUrl() == null || entity.getEntryUrl().isBlank()
                ? (config == null ? "" : Optional.ofNullable(config.entryUrl).orElse(""))
                : entity.getEntryUrl();
        return new CrawlBlueprint(
                entity.getCode(),
                entity.getName(),
                entity.isEnabled(),
                entity.getConcurrencyLimit(),
                entryUrl,
                pagingStrategy,
                flow,
                parserProfile,
                rateLimit,
                metadata
        );
    }

    private ParserProfile resolveParser(CrawlerBlueprintEntity entity, BlueprintConfig config) {
        ParserProfile profileFromConfig = config == null ? ParserProfile.empty() : buildProfile(config.parser);
        if (profileFromConfig != null && profileFromConfig.isConfigured()) {
            return profileFromConfig;
        }
        if (entity.getParserTemplateCode() != null && !entity.getParserTemplateCode().isBlank()) {
            return templateRepository.findByCode(entity.getParserTemplateCode())
                    .orElse(ParserProfile.empty());
        }
        return profileFromConfig == null ? ParserProfile.empty() : profileFromConfig;
    }

    private PagingStrategy resolvePaging(BlueprintConfig config) {
        if (config == null || config.paging == null) {
            return PagingStrategy.disabled();
        }
        BlueprintConfig.Paging paging = config.paging;
        String mode = paging.mode == null ? "NONE" : paging.mode;
        int start = paging.start == null ? 1 : paging.start;
        int step = paging.step == null ? 1 : paging.step;
        String parameter = paging.parameter;
        String sizeParameter = paging.sizeParameter;
        return PagingStrategy.from(mode, parameter, start, step, sizeParameter);
    }

    private CrawlFlow resolveFlow(BlueprintConfig config) {
        if (config == null || config.flow == null || config.flow.isEmpty()) {
            return CrawlFlow.empty();
        }
        List<CrawlStep> steps = new ArrayList<>();
        for (BlueprintConfig.FlowStep step : config.flow) {
            CrawlStepType type = CrawlStepType.REQUEST;
            if (step.type != null && !step.type.isBlank()) {
                try {
                    type = CrawlStepType.valueOf(step.type.trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                }
            }
            steps.add(new CrawlStep(type, step.options == null ? Map.of() : step.options));
        }
        return CrawlFlow.of(steps);
    }

    private CrawlBlueprint.RateLimit resolveRateLimit(BlueprintConfig config) {
        if (config == null || config.rateLimit == null) {
            return CrawlBlueprint.RateLimit.unlimited();
        }
        int rpm = config.rateLimit.requestsPerMinute == null ? 0 : config.rateLimit.requestsPerMinute;
        int burst = config.rateLimit.burst == null ? 1 : config.rateLimit.burst;
        return CrawlBlueprint.RateLimit.of(rpm, burst);
    }

    private ParserProfile buildProfile(BlueprintConfig.Parser parser) {
        if (parser == null) {
            return ParserProfile.empty();
        }
        Map<String, ParserField> fields = new LinkedHashMap<>();
        if (parser.fields != null) {
            parser.fields.forEach((name, field) -> {
                ParserFieldType type = ParserFieldType.TEXT;
                if (field.type != null) {
                    try {
                        type = ParserFieldType.valueOf(field.type.trim().toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                ParserField parserField = new ParserField(
                        name,
                        type,
                        field.selector,
                        field.attribute,
                        field.constant,
                        field.format,
                        field.delimiter,
                        Boolean.TRUE.equals(field.required)
                );
                fields.put(name, parserField);
            });
        }
        Set<String> tagFields = parser.tagFields == null ? Set.of() : new LinkedHashSet<>(parser.tagFields);
        String descriptionField = parser.descriptionField;
        return ParserProfile.of(parser.listSelector, fields, tagFields, descriptionField);
    }

    private BlueprintConfig readConfig(String json) {
        if (json == null || json.isBlank()) {
            return new BlueprintConfig();
        }
        try {
            return objectMapper.readValue(json, BlueprintConfig.class);
        } catch (IOException e) {
            log.warn("Failed to parse crawler blueprint config: {}", e.getMessage());
            return new BlueprintConfig();
        }
    }

    static class BlueprintConfig {
        public String entryUrl;
        public Paging paging;
        public Parser parser;
        public RateLimit rateLimit;
        public Map<String, Object> metadata;
        public List<FlowStep> flow;

        static class Paging {
            public String mode;
            public String parameter;
            public Integer start;
            public Integer step;
            public String sizeParameter;
        }

        static class Parser {
            public String listSelector;
            public Map<String, Field> fields;
            public List<String> tagFields;
            public String descriptionField;
        }

        static class Field {
            public String type;
            public String selector;
            public String attribute;
            public String constant;
            public String format;
            public String delimiter;
            public Boolean required;
        }

        static class RateLimit {
            public Integer requestsPerMinute;
            public Integer burst;
        }

        static class FlowStep {
            public String type;
            public Map<String, Object> options;
        }
    }
}

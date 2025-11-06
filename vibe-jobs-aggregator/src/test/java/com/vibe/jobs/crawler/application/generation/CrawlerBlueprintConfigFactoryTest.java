package com.vibe.jobs.crawler.application.generation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.jobs.crawler.domain.AutomationSettings;
import com.vibe.jobs.crawler.domain.CrawlBlueprint;
import com.vibe.jobs.crawler.domain.CrawlFlow;
import com.vibe.jobs.crawler.domain.CrawlStep;
import com.vibe.jobs.crawler.domain.CrawlStepType;
import com.vibe.jobs.crawler.domain.PagingStrategy;
import com.vibe.jobs.crawler.domain.ParserField;
import com.vibe.jobs.crawler.domain.ParserFieldType;
import com.vibe.jobs.crawler.domain.ParserProfile;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CrawlerBlueprintConfigFactoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CrawlerBlueprintConfigFactory factory = new CrawlerBlueprintConfigFactory(objectMapper);

    @Test
    void buildConfigJsonMatchesExpectedBlueprintShape() throws Exception {
        String entryUrl = "https://careers.airbnb.com/positions/?_offices=china&_locations=china";
        ParserProfile.DetailFetchConfig detailFetch = ParserProfile.DetailFetchConfig.of(
                "https://careers.airbnb.com",
                "url",
                List.of(
                        ".job-description",
                        ".entry-content",
                        "main .content",
                        "article",
                        ".position-description"
                ),
                3_000
        );

        Map<String, ParserField> fields = new LinkedHashMap<>();
        fields.put("url", new ParserField(
                "url",
                ParserFieldType.ATTRIBUTE,
                ".",
                "href",
                null,
                null,
                ",",
                true,
                "https://careers.airbnb.com"
        ));
        fields.put("title", new ParserField(
                "title",
                ParserFieldType.TEXT,
                ".",
                null,
                null,
                null,
                ",",
                true,
                null
        ));
        fields.put("company", new ParserField(
                "company",
                ParserFieldType.CONSTANT,
                "",
                null,
                "Airbnb",
                null,
                ",",
                false,
                null
        ));
        fields.put("location", new ParserField(
                "location",
                ParserFieldType.TEXT,
                ".location",
                null,
                null,
                null,
                ",",
                false,
                null
        ));
        fields.put("externalId", new ParserField(
                "externalId",
                ParserFieldType.ATTRIBUTE,
                ".",
                "href",
                null,
                null,
                ",",
                true,
                null
        ));

        ParserProfile profile = ParserProfile.of(
                "a[href*=\"/positions/\"]",
                fields,
                Set.of(),
                "",
                detailFetch
        );

        PagingStrategy paging = PagingStrategy.query("_paged", 1, 1, "");
        AutomationSettings automation = new AutomationSettings(true, false, "", 3_000, AutomationSettings.SearchSettings.disabled());
        CrawlFlow flow = CrawlFlow.of(List.of(
                new CrawlStep(CrawlStepType.WAIT, Map.of("durationMs", 5_000)),
                new CrawlStep(CrawlStepType.EXTRACT_LIST, Map.of())
        ));

        String json = factory.buildConfigJson(
                entryUrl,
                profile,
                paging,
                automation,
                flow,
                Map.of(),
                CrawlBlueprint.RateLimit.of(10, 1)
        );

        JsonNode actual = objectMapper.readTree(json);
        JsonNode expected = objectMapper.readTree("""
                {"flow": [{"type": "WAIT", "options": {"durationMs": 5000}}, {"type": "EXTRACT_LIST", "options": {}}],
                 "paging": {"mode": "QUERY", "step": 1, "start": 1, "parameter": "_paged", "sizeParameter": ""},
                 "parser": {"listSelector": "a[href*=\"/positions/\"]",
                             "baseUrl": "https://careers.airbnb.com",
                             "fields": {
                                 "url": {"type": "ATTRIBUTE", "selector": ".", "attribute": "href", "required": true, "baseUrl": "https://careers.airbnb.com"},
                                 "title": {"type": "TEXT", "selector": ".", "required": true},
                                 "company": {"type": "CONSTANT", "constant": "Airbnb"},
                                 "location": {"type": "TEXT", "selector": ".location"},
                                 "externalId": {"type": "ATTRIBUTE", "selector": ".", "attribute": "href", "required": true}
                             },
                             "tagFields": [],
                             "descriptionField": "",
                             "detailFetch": {"enabled": true,
                                              "baseUrl": "https://careers.airbnb.com",
                                              "urlField": "url",
                                              "contentSelectors": [".job-description", ".entry-content", "main .content", "article", ".position-description"],
                                              "delayMs": 3000}
                 },
                 "entryUrl": "https://careers.airbnb.com/positions/?_offices=china&_locations=china",
                 "rateLimit": {"burst": 1, "requestsPerMinute": 10},
                 "automation": {"enabled": true, "jsEnabled": false, "waitForMilliseconds": 3000}}
                """);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void omitsEmptyOptionalFieldAttributes() throws Exception {
        ParserField field = new ParserField(
                "title",
                ParserFieldType.TEXT,
                ".title",
                null,
                null,
                null,
                ",",
                false,
                null
        );
        ParserProfile profile = ParserProfile.of(
                ".job",
                Map.of("title", field),
                Set.of(),
                "",
                ParserProfile.DetailFetchConfig.disabled()
        );

        String json = factory.buildConfigJson(
                "https://example.com/jobs",
                profile,
                PagingStrategy.disabled(),
                AutomationSettings.disabled(),
                CrawlFlow.empty(),
                Map.of(),
                CrawlBlueprint.RateLimit.of(10, 1)
        );

        JsonNode node = objectMapper.readTree(json);
        JsonNode fieldNode = node.get("parser").get("fields").get("title");

        assertThat(fieldNode.get("attribute")).isNull();
        assertThat(fieldNode.get("constant")).isNull();
        assertThat(fieldNode.get("required")).isNull();
    }
}

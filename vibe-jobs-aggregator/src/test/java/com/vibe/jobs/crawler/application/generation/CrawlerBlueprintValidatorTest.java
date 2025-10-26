package com.vibe.jobs.crawler.application.generation;

import com.vibe.jobs.crawler.domain.ParserField;
import com.vibe.jobs.crawler.domain.ParserFieldType;
import com.vibe.jobs.crawler.domain.ParserProfile;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CrawlerBlueprintValidatorTest {

    private final CrawlerBlueprintValidator validator = new CrawlerBlueprintValidator();

    @Test
    void reportsFieldCoverageAndWarningsForMissingValues() {
        String html = """
                <html>
                  <body>
                    <div class='job-card'>
                      <span class='title'>Data Analyst</span>
                      <a href='/jobs/123'>View</a>
                      <span class='location'>Shanghai</span>
                    </div>
                    <div class='job-card'>
                      <span class='title'>Site Reliability Engineer</span>
                      <a>Missing href</a>
                      <span class='location'></span>
                    </div>
                  </body>
                </html>
                """;

        Map<String, ParserField> fields = new LinkedHashMap<>();
        fields.put("title", new ParserField("title", ParserFieldType.TEXT, ".title", null, null, null, ",", true, null));
        fields.put("url", new ParserField("url", ParserFieldType.ATTRIBUTE, "a", "href", null, null, ",", true, "https://example.com"));
        fields.put("location", new ParserField("location", ParserFieldType.TEXT, ".location", null, null, null, ",", false, null));

        ParserProfile profile = ParserProfile.of(
                ".job-card",
                fields,
                Set.of(),
                ""
        );

        CrawlerBlueprintValidator.ValidationResult result = validator.validate(profile, html);

        assertThat(result.success()).isTrue();
        assertThat(result.metrics()).containsKeys("jobCount", "parsedAt", "fieldCoverage");
        assertThat(result.metrics().get("jobCount")).isEqualTo(1);
        assertThat(result.samples()).hasSize(1);

        @SuppressWarnings("unchecked")
        Map<String, Object> coverage = (Map<String, Object>) result.metrics().get("fieldCoverage");
        assertThat(coverage).containsKeys("title", "url", "location");

        @SuppressWarnings("unchecked")
        Map<String, Object> urlCoverage = (Map<String, Object>) coverage.get("url");
        assertThat(urlCoverage.get("failureCount")).isEqualTo(1);
        assertThat(result.warnings()).anyMatch(warning -> warning.contains("Required field 'url'"));

        @SuppressWarnings("unchecked")
        Map<String, Object> locationCoverage = (Map<String, Object>) coverage.get("location");
        assertThat(locationCoverage.get("failureCount")).isEqualTo(1);
    }

    @Test
    void capturesParserExceptions() {
        Map<String, ParserField> fields = Map.of(
                "title", new ParserField("title", ParserFieldType.TEXT, ".title", null, null, null, ",", true, null)
        );
        ParserProfile profile = ParserProfile.of(
                "::invalid::selector",
                fields,
                Set.of(),
                ""
        );

        CrawlerBlueprintValidator.ValidationResult result = validator.validate(profile, "<html></html>");

        assertThat(result.success()).isFalse();
        assertThat(result.metrics()).containsEntry("jobCount", 0);
        assertThat(result.warnings()).anyMatch(warning -> warning.startsWith("Parser execution failed"));
    }
}

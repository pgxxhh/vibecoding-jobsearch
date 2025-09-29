package com.vibe.jobs.ingestion;

import com.vibe.jobs.config.IngestionProperties;
import com.vibe.jobs.sources.FetchedJob;
import com.vibe.jobs.sources.SourceClient;
import com.vibe.jobs.sources.SourceClientFactory;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SourceRegistryTest {

    @Test
    void shouldResolveCategoryConfiguration() {
        IngestionProperties properties = new IngestionProperties();
        properties.setCompanies(List.of("Acme Inc"));

        IngestionProperties.Source source = new IngestionProperties.Source();
        source.setId("workday-acme");
        source.setType("workday");

        Map<String, String> options = new HashMap<>();
        options.put("baseUrl", "https://{{slug}}.example.com");
        options.put("tenant", "{{slug}}");
        options.put("site", "{{slugUpper}}");
        source.setOptions(options);

        IngestionProperties.Source.CategoryQuota engineers = new IngestionProperties.Source.CategoryQuota();
        engineers.setName("{{company}} Engineers");
        engineers.setLimit(5);
        engineers.setTags(List.of("Engineering", "Software"));
        Map<String, List<String>> facets = new HashMap<>();
        facets.put("jobFamily", List.of("Engineering", "{{companyUpper}} Dev"));
        engineers.setFacets(facets);

        IngestionProperties.Source.CategoryQuota ignored = new IngestionProperties.Source.CategoryQuota();
        ignored.setName("Zero quota");
        ignored.setLimit(0);

        source.setCategories(List.of(engineers, ignored));
        properties.setSources(List.of(source));

        SourceClientFactory factory = new SourceClientFactory() {
            @Override
            public SourceClient create(String type, Map<String, String> opts) {
                return new SourceClient() {
                    @Override
                    public String sourceName() {
                        return type;
                    }

                    @Override
                    public List<FetchedJob> fetchPage(int page, int size) {
                        return List.of();
                    }
                };
            }
        };

        SourceRegistry registry = new SourceRegistry(properties, factory);
        List<SourceRegistry.ConfiguredSource> resolved = registry.getScheduledSources();
        assertThat(resolved).hasSize(1);

        SourceRegistry.ConfiguredSource configured = resolved.get(0);
        assertThat(configured.categories()).hasSize(1);
        SourceRegistry.CategoryQuota quota = configured.categories().get(0);
        assertThat(quota.name()).isEqualTo("Acme Inc Engineers");
        assertThat(quota.limit()).isEqualTo(5);
        assertThat(quota.tags()).containsExactlyInAnyOrder("engineering", "software");
        assertThat(quota.facets()).containsEntry("jobFamily", List.of("Engineering", "ACME INC Dev"));
    }
}

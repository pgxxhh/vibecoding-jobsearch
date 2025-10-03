package com.vibe.jobs.ingestion;

import com.vibe.jobs.datasource.application.DataSourceQueryService;
import com.vibe.jobs.datasource.domain.JobDataSource;
import com.vibe.jobs.datasource.domain.JobDataSource.CategoryQuotaDefinition;
import com.vibe.jobs.datasource.domain.JobDataSource.DataSourceCompany;
import com.vibe.jobs.sources.FetchedJob;
import com.vibe.jobs.sources.SourceClient;
import com.vibe.jobs.sources.SourceClientFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SourceRegistryTest {

    @Test
    void shouldResolveCategoryConfiguration() {
        DataSourceCompany company = new DataSourceCompany(
                null,
                "acme",
                "Acme Inc",
                "acme-inc",
                true,
                Map.of(),
                Map.of()
        );

        CategoryQuotaDefinition engineers = new CategoryQuotaDefinition(
                "{{company}} Engineers",
                5,
                List.of("Engineering", "Software"),
                Map.of("jobFamily", List.of("Engineering", "{{companyUpper}} Dev"))
        );
        CategoryQuotaDefinition ignored = new CategoryQuotaDefinition(
                "Zero quota",
                0,
                List.of(),
                Map.of()
        );

        JobDataSource sourceDefinition = new JobDataSource(
                null,
                "workday-acme",
                "workday",
                true,
                true,
                false,
                JobDataSource.Flow.UNLIMITED,
                Map.of(),
                List.of(engineers, ignored),
                List.of(company)
        );

        DataSourceQueryService queryService = mock(DataSourceQueryService.class);
        when(queryService.fetchAllEnabled()).thenReturn(List.of(sourceDefinition));

        SourceClientFactory factory = mock(SourceClientFactory.class);
        SourceClient mockClient = mock(SourceClient.class);
        when(mockClient.sourceName()).thenReturn("workday");
        try {
            when(mockClient.fetchPage(1, 10)).thenReturn(List.of());
        } catch (Exception e) {
            // This will never happen in the mock, but satisfies compiler
        }
        when(factory.create("workday", Map.of())).thenReturn(mockClient);

        SourceRegistry registry = new SourceRegistry(queryService, factory);
        List<SourceRegistry.ConfiguredSource> resolved = registry.getScheduledSources();
        assertThat(resolved).hasSize(1);

        SourceRegistry.ConfiguredSource configured = resolved.get(0);
        assertThat(configured.definition().getCode()).isEqualTo("workday-acme");
        assertThat(configured.categories()).hasSize(1);

        SourceRegistry.CategoryQuota quota = configured.categories().get(0);
        assertThat(quota.name()).isEqualTo("Acme Inc Engineers");
        assertThat(quota.limit()).isEqualTo(5);
        assertThat(quota.tags()).containsExactlyInAnyOrder("engineering", "software");
        assertThat(quota.facets()).containsEntry("jobFamily", List.of("Engineering", "ACME INC Dev"));
    }
}

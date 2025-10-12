package com.vibe.jobs.ingestion;

import com.vibe.jobs.admin.domain.event.DataSourceConfigurationChangedEvent;
import com.vibe.jobs.config.IngestionProperties;
import com.vibe.jobs.datasource.application.DataSourceQueryService;
import com.vibe.jobs.domain.Job;
import com.vibe.jobs.sources.FetchedJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JobIngestionFilterTest {

    private IngestionProperties properties;
    private DataSourceQueryService queryService;
    private JobIngestionFilter filter;

    @BeforeEach
    void setUp() {
        properties = new IngestionProperties();
        properties.setRecentDays(30);
        queryService = mock(DataSourceQueryService.class);
        filter = new JobIngestionFilter(properties, queryService);
    }

    @Test
    void shouldUseCachedCompanyListWhenCacheIsValid() {
        when(queryService.getNormalizedCompanyNames()).thenReturn(Set.of("acme"));

        FetchedJob job = createJob("Acme");

        List<FetchedJob> firstResult = filter.apply(List.of(job));
        List<FetchedJob> secondResult = filter.apply(List.of(job));

        assertThat(firstResult).hasSize(1);
        assertThat(secondResult).hasSize(1);
        verify(queryService, times(1)).getNormalizedCompanyNames();
    }

    @Test
    void shouldRefreshCompanyListWhenCacheExpired() throws Exception {
        when(queryService.getNormalizedCompanyNames())
                .thenReturn(Set.of("acme"))
                .thenReturn(Set.of("globex"));

        filter.apply(List.of(createJob("Acme")));

        setLastRefresh(filter, Instant.now().minus(Duration.ofHours(1)));

        List<FetchedJob> refreshed = filter.apply(List.of(createJob("Globex")));

        assertThat(refreshed).hasSize(1);
        verify(queryService, times(2)).getNormalizedCompanyNames();
    }

    @Test
    void shouldClearCacheOnDataSourceConfigurationChange() {
        when(queryService.getNormalizedCompanyNames())
                .thenReturn(Set.of("acme"))
                .thenReturn(Set.of("globex"));

        filter.apply(List.of(createJob("Acme")));

        filter.onDataSourceConfigurationChanged(new DataSourceConfigurationChangedEvent("any"));

        List<FetchedJob> refreshed = filter.apply(List.of(createJob("Globex")));

        assertThat(refreshed).hasSize(1);
        verify(queryService, times(2)).getNormalizedCompanyNames();
    }

    private FetchedJob createJob(String company) {
        Job job = Job.builder()
                .source("source")
                .externalId("id")
                .title("title")
                .company(company)
                .postedAt(Instant.now())
                .build();
        return FetchedJob.of(job, "");
    }

    @SuppressWarnings("unchecked")
    private void setLastRefresh(JobIngestionFilter filter, Instant instant) throws Exception {
        Field field = JobIngestionFilter.class.getDeclaredField("enabledCompaniesLastRefresh");
        field.setAccessible(true);
        AtomicReference<Instant> reference = (AtomicReference<Instant>) field.get(filter);
        reference.set(instant);
    }
}


package com.vibe.jobs.ingestion;

import com.vibe.jobs.admin.domain.event.DataSourceConfigurationChangedEvent;
import com.vibe.jobs.config.IngestionProperties;
import com.vibe.jobs.datasource.application.DataSourceQueryService;
import com.vibe.jobs.domain.Job;
import com.vibe.jobs.sources.FetchedJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobIngestionFilterTest {

    @Mock
    private DataSourceQueryService queryService;

    private JobIngestionFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JobIngestionFilter(new IngestionProperties(), queryService);
    }

    @Test
    void shouldUseCachedCompaniesWithinTtl() {
        when(queryService.getNormalizedCompanyNames()).thenReturn(Set.of("acme"));

        List<FetchedJob> jobs = List.of(fetchedJob("Acme", Instant.now()));

        List<FetchedJob> firstCall = filter.apply(jobs);
        List<FetchedJob> secondCall = filter.apply(jobs);

        assertThat(firstCall).hasSize(1);
        assertThat(secondCall).hasSize(1);
        verify(queryService, times(1)).getNormalizedCompanyNames();
    }

    @Test
    void shouldRefreshCacheWhenExpired() {
        when(queryService.getNormalizedCompanyNames())
                .thenReturn(Set.of("acme"))
                .thenReturn(Set.of("beta"));

        List<FetchedJob> jobs = List.of(fetchedJob("Acme", Instant.now()));

        filter.apply(jobs);

        @SuppressWarnings("unchecked")
        AtomicReference<Instant> lastRefreshRef = (AtomicReference<Instant>) ReflectionTestUtils
                .getField(filter, "enabledCompaniesCacheRefreshedAt");
        assertThat(lastRefreshRef).isNotNull();
        lastRefreshRef.set(Instant.now().minus(Duration.ofMinutes(10)));

        filter.apply(jobs);

        verify(queryService, times(2)).getNormalizedCompanyNames();
    }

    @Test
    void shouldInvalidateCacheOnDataSourceChangeEvent() {
        when(queryService.getNormalizedCompanyNames()).thenReturn(Set.of("acme"));

        List<FetchedJob> jobs = List.of(fetchedJob("Acme", Instant.now()));

        filter.apply(jobs);
        filter.onDataSourceConfigurationChanged(new DataSourceConfigurationChangedEvent("test"));
        filter.apply(jobs);

        verify(queryService, times(2)).getNormalizedCompanyNames();
    }

    private FetchedJob fetchedJob(String company, Instant postedAt) {
        Job job = Job.builder()
                .source("test")
                .externalId("ext-" + company)
                .title("Engineer")
                .company(company)
                .postedAt(postedAt)
                .build();
        return FetchedJob.of(job, "content");
    }
}

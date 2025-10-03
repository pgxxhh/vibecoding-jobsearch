package com.vibe.jobs.ingestion;

import com.vibe.jobs.config.IngestionProperties;
import com.vibe.jobs.datasource.application.DataSourceQueryService;
import com.vibe.jobs.datasource.domain.JobDataSource;
import com.vibe.jobs.domain.Job;
import com.vibe.jobs.service.JobDetailService;
import com.vibe.jobs.service.JobService;
import com.vibe.jobs.service.LocationFilterService;
import com.vibe.jobs.service.RoleFilterService;
import com.vibe.jobs.sources.FetchedJob;
import com.vibe.jobs.sources.SourceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobIngestionSchedulerTest {

    @Mock
    private JobService jobService;

    @Mock
    private JobDetailService jobDetailService;

    @Mock
    private LocationFilterService locationFilterService;

    @Mock
    private RoleFilterService roleFilterService;

    @Mock
    private DataSourceQueryService dataSourceQueryService;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void shouldRespectCategoryQuotas() {
        IngestionProperties properties = new IngestionProperties();
        properties.setPageSize(2);

        JobIngestionFilter filter = new JobIngestionFilter(properties, dataSourceQueryService);

        StubSourceClient client = new StubSourceClient();
        client.addPage(List.of(
                fetchedJob("eng1", Set.of("Engineering")),
                fetchedJob("fin1", Set.of("Finance"))
        ));
        client.addPage(List.of(
                fetchedJob("fin2", Set.of("Finance")),
                fetchedJob("eng2", Set.of("Engineering"))
        ));

        JobDataSource definition = new JobDataSource(
                null,
                "stub",
                "greenhouse",
                true,
                true,
                false,
                JobDataSource.Flow.UNLIMITED,
                Map.of(),
                List.of(),
                List.of()
        );

        List<SourceRegistry.CategoryQuota> quotas = List.of(
                new SourceRegistry.CategoryQuota("engineer", 1, List.of("engineering"), java.util.Map.of()),
                new SourceRegistry.CategoryQuota("finance", 2, List.of("finance"), java.util.Map.of())
        );
        SourceRegistry.ConfiguredSource configuredSource = new SourceRegistry.ConfiguredSource(
                definition,
                "Acme",
                client,
                quotas
        );

        SourceRegistry registry = mock(SourceRegistry.class);
        when(registry.getScheduledSources()).thenReturn(List.of(configuredSource));

        when(jobService.upsert(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(jobDetailService).saveContent(any(), anyString());
        
        // Mock location filter service to return all jobs unchanged
        when(locationFilterService.filterJobs(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock role filter service to return all jobs unchanged  
        when(roleFilterService.filter(any())).thenAnswer(invocation -> invocation.getArgument(0));

        JobIngestionScheduler scheduler = new JobIngestionScheduler(
                jobService,
                properties,
                registry,
                filter,
                jobDetailService,
                locationFilterService,
                roleFilterService,
                executor
        );

        scheduler.runIngestion();

        assertThat(client.requestedPages()).containsExactly(1, 2);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobService, times(3)).upsert(captor.capture());
        List<String> externalIds = captor.getAllValues().stream().map(Job::getExternalId).toList();
        assertThat(externalIds).containsExactlyInAnyOrder("eng1", "fin1", "fin2");

        verify(jobDetailService, times(3)).saveContent(any(), anyString());
    }

    private FetchedJob fetchedJob(String externalId, Set<String> tags) {
        Job job = Job.builder()
                .source("stub")
                .externalId(externalId)
                .title(externalId)
                .company("Acme")
                .postedAt(Instant.now())
                .tags(new HashSet<>(tags))
                .build();
        return FetchedJob.of(job, externalId + "-content");
    }

    private static class StubSourceClient implements SourceClient {
        private final List<List<FetchedJob>> pages = new ArrayList<>();
        private final List<Integer> requestedPages = new ArrayList<>();

        void addPage(List<FetchedJob> jobs) {
            pages.add(jobs);
        }

        List<Integer> requestedPages() {
            return requestedPages;
        }

        @Override
        public String sourceName() {
            return "stub";
        }

        @Override
        public List<FetchedJob> fetchPage(int page, int size) {
            requestedPages.add(page);
            int index = page - 1;
            if (index >= 0 && index < pages.size()) {
                return pages.get(index);
            }
            return List.of();
        }
    }
}

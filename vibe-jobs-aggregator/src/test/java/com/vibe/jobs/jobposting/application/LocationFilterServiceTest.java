package com.vibe.jobs.jobposting.application;

import com.vibe.jobs.shared.infrastructure.config.IngestionProperties;
import com.vibe.jobs.jobposting.domain.Job;
import com.vibe.jobs.ingestion.infrastructure.sourceclient.FetchedJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocationFilterServiceTest {

    private LocationFilterService locationFilterService;
    private IngestionProperties properties;

    @BeforeEach
    void setUp() {
        properties = new IngestionProperties();
        locationFilterService = new LocationFilterService(properties);
    }

    @Test
    void testLocationFilterDisabled() {
        // Given: location filter is disabled
        IngestionProperties.LocationFilter filter = properties.getLocationFilter();
        filter.setEnabled(false);

        List<FetchedJob> jobs = createTestJobs();

        // When: filter jobs
        List<FetchedJob> result = locationFilterService.filterJobs(jobs);

        // Then: all jobs should be returned
        assertEquals(jobs.size(), result.size());
    }

    @Test
    void testLocationFilterEnabled() {
        // Given: location filter is enabled with China cities
        IngestionProperties.LocationFilter filter = properties.getLocationFilter();
        filter.setEnabled(true);
        filter.setIncludeCities(List.of("beijing", "shanghai", "shenzhen"));

        List<FetchedJob> jobs = createTestJobs();

        // When: filter jobs
        List<FetchedJob> result = locationFilterService.filterJobs(jobs);

        // Then: only jobs with matching locations should be returned
        assertEquals(2, result.size()); // Beijing and Shanghai jobs
        assertTrue(result.stream().anyMatch(job -> job.job().getLocation().contains("Beijing")));
        assertTrue(result.stream().anyMatch(job -> job.job().getLocation().contains("Shanghai")));
    }

    @Test
    void testLocationFilterWithExclusions() {
        // Given: location filter excludes US locations
        IngestionProperties.LocationFilter filter = properties.getLocationFilter();
        filter.setEnabled(true);
        filter.setExcludeKeywords(List.of("remote - us", "new york"));

        List<FetchedJob> jobs = createTestJobs();

        // When: filter jobs
        List<FetchedJob> result = locationFilterService.filterJobs(jobs);

        // Then: US jobs should be filtered out
        assertEquals(3, result.size()); // All except New York
        assertFalse(result.stream().anyMatch(job -> job.job().getLocation().contains("New York")));
    }

    @Test
    void testLocationFilterStatus() {
        // Given: location filter is configured
        IngestionProperties.LocationFilter filter = properties.getLocationFilter();
        filter.setEnabled(true);
        filter.setIncludeCities(List.of("beijing", "shanghai"));

        // When: get filter status
        String status = locationFilterService.getFilterStatus();

        // Then: status should contain configuration details
        assertTrue(status.contains("ENABLED"));
        assertTrue(status.contains("beijing"));
        assertTrue(status.contains("shanghai"));
    }

    private List<FetchedJob> createTestJobs() {
        return List.of(
                createFetchedJob("Software Engineer", "Beijing, China"),
                createFetchedJob("Product Manager", "Shanghai, China"),
                createFetchedJob("Data Scientist", "New York, NY"),
                createFetchedJob("Frontend Developer", "Singapore")
        );
    }

    private FetchedJob createFetchedJob(String title, String location) {
        Job job = Job.builder()
                .source("test")
                .externalId("test-" + title.hashCode())
                .title(title)
                .company("TestCorp")
                .location(location)
                .level("mid")
                .postedAt(Instant.now())
                .url("http://example.com")
                .checksum("test-checksum")
                .build();
        return new FetchedJob(job, "Test job description");
    }
}
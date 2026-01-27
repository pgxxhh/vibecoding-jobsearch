package com.vibe.jobs.jobposting.application;

import com.vibe.jobs.ingestion.infrastructure.sourceclient.FetchedJob;
import com.vibe.jobs.jobposting.domain.Job;
import com.vibe.jobs.shared.infrastructure.config.IngestionProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JobFilterMatcherTest {

    @Test
    void matchesLocationRespectsIncludeKeywords() {
        IngestionProperties.LocationFilter filter = new IngestionProperties.LocationFilter();
        filter.setEnabled(true);
        filter.setIncludeCountries(List.of("singapore"));

        assertThat(JobFilterMatcher.matchesLocation("Singapore", filter)).isTrue();
        assertThat(JobFilterMatcher.matchesLocation("Tokyo", filter)).isFalse();
    }

    @Test
    void matchesRoleUsesIncludeExcludeKeywords() {
        IngestionProperties.RoleFilter filter = new IngestionProperties.RoleFilter();
        filter.setEnabled(true);
        filter.setIncludeKeywords(List.of("engineer"));
        filter.setExcludeKeywords(List.of("intern"));

        Job validJob = Job.builder()
                .title("Senior Software Engineer")
                .build();
        Job invalidJob = Job.builder()
                .title("Software Engineer Intern")
                .build();

        assertThat(JobFilterMatcher.matchesRole(new FetchedJob(validJob, ""), filter)).isTrue();
        assertThat(JobFilterMatcher.matchesRole(new FetchedJob(invalidJob, ""), filter)).isFalse();
    }
}

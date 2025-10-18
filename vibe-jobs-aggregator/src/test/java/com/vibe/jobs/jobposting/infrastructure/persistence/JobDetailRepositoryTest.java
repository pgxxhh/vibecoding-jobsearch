package com.vibe.jobs.jobposting.infrastructure.persistence;

import com.vibe.jobs.jobposting.domain.Job;
import com.vibe.jobs.jobposting.domain.JobDetail;
import com.vibe.jobs.jobposting.domain.JobDetailEnrichment;
import com.vibe.jobs.jobposting.domain.JobEnrichmentKey;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class JobDetailRepositoryTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobDetailRepository jobDetailRepository;

    @Test
    void findMatchingJobIdsHandlesMultipleKeywordsAndCaseInsensitivity() {
        Job job = createJob("detail-1", "Backend Engineer");
        jobRepository.save(job);

        JobDetail detail = new JobDetail(job,
                "<p>GO experts wanted for distributed systems</p>",
                "GO experts wanted for distributed systems");
        jobDetailRepository.save(detail);

        Set<Long> lowerCaseMatches = jobDetailRepository.findMatchingJobIds(List.of(job.getId()), "go experts");
        assertEquals(Set.of(job.getId()), lowerCaseMatches);

        Set<Long> upperCaseMatches = jobDetailRepository.findMatchingJobIds(List.of(job.getId()), "GO EXPERTS");
        assertEquals(Set.of(job.getId()), upperCaseMatches);
    }

    @Test
    void findMatchingJobIdsFallsBackWhenFullTextUnsupported() {
        Job job1 = createJob("detail-2", "Python Developer");
        Job job2 = createJob("detail-3", "Go Developer");
        jobRepository.save(job1);
        jobRepository.save(job2);

        jobDetailRepository.save(new JobDetail(job1,
                "<p>We need a Python developer with cloud experience</p>",
                "We need a Python developer with cloud experience"));
        jobDetailRepository.save(new JobDetail(job2,
                "<p>We need a Go specialist</p>",
                "We need a Go specialist"));

        Set<Long> matches = jobDetailRepository.findMatchingJobIds(List.of(job1.getId(), job2.getId()), "Python developer");
        assertEquals(Set.of(job1.getId()), matches);

        Set<Long> filteredMatches = jobDetailRepository.findMatchingJobIds(List.of(job2.getId()), "Python developer");
        assertTrue(filteredMatches.isEmpty(), "Should respect provided job id filter");
    }

    private Job createJob(String externalId, String title) {
        return Job.builder()
                .source("test")
                .externalId(externalId)
                .title(title)
                .company("TestCo")
                .location("Remote")
                .level("mid")
                .postedAt(Instant.now())
                .url("http://example.com/" + externalId)
                .checksum("checksum-" + externalId)
                .build();
    }
}


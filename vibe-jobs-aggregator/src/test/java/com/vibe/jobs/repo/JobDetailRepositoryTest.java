package com.vibe.jobs.repo;

import com.vibe.jobs.domain.Job;
import com.vibe.jobs.domain.JobDetail;
import com.vibe.jobs.domain.JobDetailEnrichment;
import com.vibe.jobs.domain.JobEnrichmentKey;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
class JobDetailRepositoryTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobDetailRepository jobDetailRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    @Test
    void findEnrichmentsByJobIdsReturnsProjectionWithoutLoadingEntity() {
        Job job = jobRepository.save(Job.builder()
                .source("test")
                .externalId("ext-1")
                .title("Software Engineer")
                .company("Example Corp")
                .location("Remote")
                .level("senior")
                .postedAt(Instant.now())
                .url("https://example.com/job/1")
                .checksum("checksum-1")
                .build());

        JobDetail detail = new JobDetail(job, "<p>content</p>", "content");
        JobDetailEnrichment enrichment = detail.upsertEnrichment(JobEnrichmentKey.SUMMARY);
        enrichment.updateValue("\"short summary\"", "provider", "fingerprint", null, null);
        jobDetailRepository.saveAndFlush(detail);

        statistics.clear();

        List<JobDetailRepository.EnrichmentView> views = jobDetailRepository.findEnrichmentsByJobIds(Set.of(job.getId()));

        assertThat(views).hasSize(1);
        JobDetailRepository.EnrichmentView view = views.get(0);
        assertThat(view.getJobId()).isEqualTo(job.getId());
        assertThat(view.getEnrichmentKey()).isEqualTo(JobEnrichmentKey.SUMMARY);
        assertThat(view.getValueJson()).isEqualTo("\"short summary\"");
        assertThat(statistics.getEntityLoadCount()).isZero();
    }
}

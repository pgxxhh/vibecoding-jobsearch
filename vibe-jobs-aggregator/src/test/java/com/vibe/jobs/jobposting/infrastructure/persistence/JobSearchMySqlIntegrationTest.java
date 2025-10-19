package com.vibe.jobs.jobposting.infrastructure.persistence;

import com.vibe.jobs.jobposting.domain.Job;
import com.vibe.jobs.jobposting.domain.JobDetail;
import com.vibe.jobs.jobposting.domain.spi.JobDetailRepositoryPort;
import com.vibe.jobs.jobposting.domain.spi.JobRepositoryPort;
import com.vibe.jobs.jobposting.infrastructure.persistence.JobJpaRepository;
import com.vibe.jobs.jobposting.infrastructure.persistence.JobJpaRepositoryImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@ActiveProfiles("mysql")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JobSearchMySqlIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("vibejobs")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private JobRepositoryPort jobRepository;

    @Autowired
    private JobDetailRepositoryPort jobDetailRepository;

    @Autowired
    private JobDetailJpaRepository jobDetailJpaRepository;

    @Autowired
    private JobJpaRepository jobJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void cleanDatabase() {
        jobDetailJpaRepository.deleteAll();
        jobJpaRepository.deleteAll();
    }

    @Test
    void primaryFieldSearchUsesFullTextAndPostedAtIndex() {
        Job job1 = saveJob("p1", "Staff Software Engineer", "Alpha", "Remote", "staff", Instant.parse("2024-05-01T10:00:00Z"), "kubernetes");
        Job job2 = saveJob("p2", "Senior Data Engineer", "Beta", "Austin", "senior", Instant.parse("2024-05-02T10:00:00Z"));
        saveJob("p3", "Marketing Manager", "Gamma", "New York", "manager", Instant.parse("2024-05-03T10:00:00Z"));

        List<Job> results = jobRepository.searchAfter(
                "Engineer", null, null, null, null, null, null, false, 0, 10);

        assertThat(results)
                .extracting(Job::getId)
                .containsExactly(job2.getId(), job1.getId());

        assertPlanUsesPostedAtIndex("Engineer", null, null, null, null, null, null, false);
    }

    @Test
    void detailSearchMatchesOnlyWhenEnabled() {
        Job job = saveJob("d1", "Backend Developer", "Infra", "Remote", "mid", Instant.parse("2024-06-01T09:00:00Z"));
        jobDetailRepository.save(new JobDetail(job, "<p>Kubernetes experts wanted</p>", "Kubernetes experts wanted"));

        assertTrue(jobRepository.searchAfter("Kubernetes", null, null, null, null, null, null, false, 0, 10).isEmpty());

        List<Job> results = jobRepository.searchAfter(
                "Kubernetes", null, null, null, null, null, null, true, 0, 10);

        assertThat(results)
                .extracting(Job::getId)
                .containsExactly(job.getId());

        assertEquals(1L, jobRepository.countSearch("Kubernetes", null, null, null, null, true));
    }

    @Test
    void searchSupportsMixedFilters() {
        saveJob("m1", "DevOps Engineer", "Cloudy", "Seattle", "senior", Instant.parse("2024-07-01T08:00:00Z"));
        Job matching = saveJob("m2", "DevOps Engineer", "Cloudy", "Remote", "senior", Instant.parse("2024-07-02T08:00:00Z"));
        saveJob("m3", "DevOps Engineer", "Other", "Remote", "junior", Instant.parse("2024-07-03T08:00:00Z"));

        List<Job> results = jobRepository.searchAfter(
                "DevOps", "Cloudy", "Remote", "senior", null, null, null, false, 0, 10);

        assertThat(results)
                .extracting(Job::getId)
                .containsExactly(matching.getId());
    }

    @Test
    void cursorPaginationReturnsStablePages() {
        Job newest = saveJob("c1", "QA Engineer", "Acme", "Remote", "mid", Instant.parse("2024-08-03T12:00:00Z"));
        Job middle = saveJob("c2", "QA Engineer", "Acme", "Remote", "mid", Instant.parse("2024-08-02T12:00:00Z"));
        Job oldest = saveJob("c3", "QA Engineer", "Acme", "Remote", "mid", Instant.parse("2024-08-01T12:00:00Z"));

        List<Job> firstPage = jobRepository.searchAfter(
                "QA", "Acme", "Remote", null, null, null, null, false, 0, 2);

        assertThat(firstPage)
                .extracting(Job::getId)
                .containsExactly(newest.getId(), middle.getId());

        Job lastOfFirstPage = firstPage.get(firstPage.size() - 1);
        List<Job> secondPage = jobRepository.searchAfter(
                "QA", "Acme", "Remote", null,
                null,
                lastOfFirstPage.getPostedAt(),
                lastOfFirstPage.getId(),
                false,
                0,
                2);

        assertThat(secondPage)
                .extracting(Job::getId)
                .containsExactly(oldest.getId());
    }

    private Job saveJob(String externalId,
                        String title,
                        String company,
                        String location,
                        String level,
                        Instant postedAt,
                        String... tags) {
        Job job = Job.builder()
                .source("integration")
                .externalId(externalId)
                .title(title)
                .company(company)
                .location(location)
                .level(level)
                .postedAt(postedAt)
                .url("http://example.com/" + externalId)
                .checksum("checksum-" + externalId)
                .build();
        if (tags != null && tags.length > 0) {
            job.setTags(Set.of(tags));
        }
        return jobRepository.save(job);
    }

    private void assertPlanUsesPostedAtIndex(String q,
                                             String company,
                                             String location,
                                             String level,
                                             Instant postedAfter,
                                             Instant cursorPostedAt,
                                             Long cursorId,
                                             boolean searchDetail) {
        JobJpaRepositoryImpl repositoryImpl = new JobJpaRepositoryImpl(entityManager);
        String normalizedQuery = (String) ReflectionTestUtils.invokeMethod(repositoryImpl, "normalize", q);
        boolean hasQuery = normalizedQuery != null;
        boolean detailEnabled = searchDetail && hasQuery;
        boolean hasCursor = cursorPostedAt != null && cursorId != null;
        String sql = (String) ReflectionTestUtils.invokeMethod(repositoryImpl, "buildSearchSql",
                false, detailEnabled, hasCursor, hasQuery);
        Map<String, Object> params = new HashMap<>();
        String fullTextQuery = null;
        Boolean supportsFullText = (Boolean) ReflectionTestUtils.getField(repositoryImpl, "supportsFullText");
        if (Boolean.TRUE.equals(supportsFullText) && hasQuery) {
            fullTextQuery = (String) ReflectionTestUtils.invokeMethod(repositoryImpl, "buildFullTextQuery", normalizedQuery);
        }
        ReflectionTestUtils.invokeMethod(repositoryImpl, "populateCommonParameters", params,
                normalizedQuery, fullTextQuery, company, location, level, postedAfter, detailEnabled);
        if (hasCursor) {
            params.put("cursorPostedAt", Timestamp.from(cursorPostedAt));
            params.put("cursorId", cursorId);
        }
        Query explain = entityManager.createNativeQuery("EXPLAIN " + sql);
        ReflectionTestUtils.invokeMethod(repositoryImpl, "applyParameters", explain, params);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = explain.getResultList();
        assertFalse(rows.isEmpty(), "EXPLAIN should return at least one row");
        Object[] firstRow = rows.get(0);
        String key = firstRow[6] != null ? firstRow[6].toString() : null;
        assertEquals("idx_jobs_posted_at_id_desc", key, "Expected posted_at/id index to be used");
    }
}

package com.vibe.jobs.repo;

import com.vibe.jobs.domain.Job;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("h2")
class JobSearchTest {

    @Autowired
    private JobRepository jobRepository;

    @Test
    void testKeywordSearchInAllFields() {
        // Given: Create test jobs with different field content
        Job job1 = createJob("1", "Software Engineer", "TechCorp", "San Francisco", "engineering");
        Job job2 = createJob("2", "Product Manager", "DataCorp", "New York", "product");
        Job job3 = createJob("3", "Frontend Developer", "WebCorp", "Remote", "frontend", "javascript");

        jobRepository.save(job1);
        jobRepository.save(job2);
        jobRepository.save(job3);

        // Test 1: Search by title keyword
        var results1 = jobRepository.searchAfter("Software", null, null, null, null, null, null, PageRequest.of(0, 10));
        assertEquals(1, results1.size());
        assertEquals("Software Engineer", results1.get(0).getTitle());

        // Test 2: Search by company keyword  
        var results2 = jobRepository.searchAfter("TechCorp", null, null, null, null, null, null, PageRequest.of(0, 10));
        assertEquals(1, results2.size());
        assertEquals("TechCorp", results2.get(0).getCompany());

        // Test 3: Search by location keyword
        var results3 = jobRepository.searchAfter("Francisco", null, null, null, null, null, null, PageRequest.of(0, 10));
        assertEquals(1, results3.size());
        assertEquals("San Francisco", results3.get(0).getLocation());

        // Test 4: Search by tag keyword
        var results4 = jobRepository.searchAfter("javascript", null, null, null, null, null, null, PageRequest.of(0, 10));
        assertEquals(1, results4.size());
        assertEquals("Frontend Developer", results4.get(0).getTitle());

        // Test 5: Search for partial match should work across all fields
        var results5 = jobRepository.searchAfter("Corp", null, null, null, null, null, null, PageRequest.of(0, 10));
        assertEquals(3, results5.size()); // Should find TechCorp, DataCorp, WebCorp
    }

    private Job createJob(String id, String title, String company, String location, String... tags) {
        Job job = Job.builder()
                .source("test")
                .externalId(id)
                .title(title)
                .company(company)
                .location(location)
                .level("mid")
                .postedAt(Instant.now())
                .url("http://example.com/" + id)
                .checksum("checksum" + id)
                .build();
        
        if (tags != null && tags.length > 0) {
            job.setTags(Set.of(tags));
        }
        
        return job;
    }
}
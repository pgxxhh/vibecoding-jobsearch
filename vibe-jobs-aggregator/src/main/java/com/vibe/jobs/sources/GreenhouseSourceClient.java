
package com.vibe.jobs.sources;

import com.vibe.jobs.domain.Job;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Fetch jobs from Greenhouse: https://boards-api.greenhouse.io/v1/boards/{company}/jobs
 * Many companies host their careers on Greenhouse; start with company slugs like "stripe", "datadog", etc.
 */
public class GreenhouseSourceClient implements SourceClient {

    private final String company;  // greenhouse board slug, e.g., "stripe"
    private final WebClient client;

    public GreenhouseSourceClient(String company) {
        this.company = company;
        this.client = WebClient.builder()
                .baseUrl("https://boards-api.greenhouse.io/v1/boards/" + company)
                .build();
    }

    @Override
    public String sourceName() {
        return "greenhouse:" + company;
    }

    @Override
    public List<Job> fetchPage(int page, int size) throws Exception {
        Map<String, Object> response = client.get()
                .uri("/jobs?content=true")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("jobs")) return List.of();

        List<Map<String, Object>> jobs = (List<Map<String, Object>>) response.get("jobs");

        return jobs.stream().map(j -> {
            String externalId = String.valueOf(j.get("id"));
            String title = (String) j.get("title");
            String location = null;
            Object locObj = j.get("location");
            if (locObj instanceof Map<?,?> locMap) {
                Object name = locMap.get("name");
                location = name == null ? null : name.toString();
            }
            String url = (String) j.get("absolute_url");

            return Job.builder()
                    .source(sourceName())
                    .externalId(externalId)
                    .title(title)
                    .company(company)
                    .location(location)
                    .postedAt(Instant.now()) // Greenhouse doesn't provide posted time in this endpoint
                    .url(url)
                    .tags(new HashSet<>())
                    .build();
        }).collect(Collectors.toList());
    }
}

package com.vibe.jobs.ingestion.infrastructure.sourceclient;

import com.vibe.jobs.jobposting.domain.Job;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fetch jobs from Lever: https://api.lever.co/v0/postings/{company}?mode=json
 */
public class LeverSourceClient implements SourceClient {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_OF_MAP =
            new ParameterizedTypeReference<>() {};

    private final String company;
    private final WebClient client;

    public LeverSourceClient(String company) {
        this.company = company;
        this.client = WebClient.builder()
                .baseUrl("https://api.lever.co/v0/postings/" + company)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)")
                .codecs(config -> config.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }

    @Override
    public String sourceName() {
        return "lever:" + company;
    }

    @Override
    public List<FetchedJob> fetchPage(int page, int size) {
        int perPage = Math.max(1, Math.min(size, 100));
        int skip = Math.max(page - 1, 0) * perPage;

        List<Map<String, Object>> response;
        try {
            response = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("mode", "json")
                            .queryParam("limit", perPage)
                            .queryParam("skip", skip)
                            .build())
                    .retrieve()
                    .bodyToMono(LIST_OF_MAP)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            return List.of();
        }

        if (response == null || response.isEmpty()) {
            return List.of();
        }

        return response.stream()
                .map(this::mapJob)
                .toList();
    }

    private FetchedJob mapJob(Map<String, Object> entry) {
        String jobId = stringValue(entry.get("id"));
        String title = stringValue(entry.get("text"));
        String location = null;
        Object categories = entry.get("categories");
        if (categories instanceof Map<?, ?> categoriesMap) {
            Object locationValue = categoriesMap.get("location");
            location = locationValue == null ? null : String.valueOf(locationValue);
        }

        Instant postedAt = parseInstant(entry.get("createdAt"));

        Set<String> tags = new HashSet<>();
        Object departments = entry.get("departments");
        if (departments instanceof List<?> deptList) {
            for (Object dept : deptList) {
                String value = stringValue(dept);
                if (!value.isEmpty()) {
                    tags.add(value);
                }
            }
        }

        String url = stringValue(entry.get("hostedUrl"));

        Job job = Job.builder()
                .source(sourceName())
                .externalId(jobId.isEmpty() ? title : jobId)
                .title(title)
                .company(company)
                .location(location)
                .postedAt(postedAt)
                .url(url)
                .tags(tags)
                .build();
        String content = stringValue(entry.get("description"));
        if (content.isEmpty() && entry.containsKey("content")) {
            Object contentObj = entry.get("content");
            if (contentObj instanceof Map<?, ?> contentMap) {
                content = stringValue(contentMap.get("descriptionHtml"));
                if (content.isEmpty()) {
                    content = stringValue(contentMap.get("descriptionText"));
                }
            } else {
                content = stringValue(contentObj);
            }
        }
        return new FetchedJob(job, content);
    }

    private Instant parseInstant(Object value) {
        if (value instanceof Number number) {
            return Instant.ofEpochMilli(number.longValue());
        }
        if (value instanceof String s) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                try {
                    return Instant.parse(trimmed);
                } catch (DateTimeParseException ignored) {
                }
            }
        }
        return Instant.now();
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}

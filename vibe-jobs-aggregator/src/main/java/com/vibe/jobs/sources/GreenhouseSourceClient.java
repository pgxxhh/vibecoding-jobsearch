
package com.vibe.jobs.sources;

import com.vibe.jobs.domain.Job;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.time.format.DateTimeParseException;
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
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)")
                .codecs(config -> config.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }

    @Override
    public String sourceName() {
        return "greenhouse:" + company;
    }

    @Override
    public List<FetchedJob> fetchPage(int page, int size) throws Exception {
        int perPage = Math.max(1, Math.min(size, 50));
        int pageNumber = Math.max(page, 1);

        Map<String, Object> response = fetchJobsIndex();
        if (response == null || !response.containsKey("jobs")) {
            return List.of();
        }

        List<Map<String, Object>> jobs = (List<Map<String, Object>>) response.get("jobs");
        if (jobs == null || jobs.isEmpty()) {
            return List.of();
        }

        int fromIndex = Math.min((pageNumber - 1) * perPage, jobs.size());
        if (fromIndex >= jobs.size()) {
            return List.of();
        }
        int toIndex = Math.min(fromIndex + perPage, jobs.size());

        List<Map<String, Object>> slice = jobs.subList(fromIndex, toIndex);

        return slice.stream()
                .map(job -> mapJob(job, fetchJobDetail(job)))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Map<String, Object> fetchJobsIndex() {
        try {
            return client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/jobs")
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            return null;
        }
    }

    private Map<String, Object> fetchJobDetail(Map<String, Object> summary) {
        Object id = summary == null ? null : summary.get("id");
        if (id == null) {
            return summary;
        }
        try {
            Map<String, Object> detail = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/jobs/" + id)
                            .queryParam("content", true)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (detail == null) {
                return summary;
            }
            if (detail.containsKey("job") && detail.get("job") instanceof Map<?, ?> jobMap) {
                return (Map<String, Object>) jobMap;
            }
            return detail;
        } catch (WebClientResponseException.NotFound e) {
            return summary;
        }
    }

    private FetchedJob mapJob(Map<String, Object> summary, Map<String, Object> detail) {
        Map<String, Object> data = detail != null ? detail : summary;
        if (data == null) {
            return null;
        }

        String id = string(summary.get("id"));
        String title = string(data.getOrDefault("title", summary.get("title")));
        String location = extractLocation(data, summary);
        String url = string(data.getOrDefault("absolute_url", summary.get("absolute_url")));
        Instant postedAt = extractPostedAt(data, summary);
        Set<String> tags = extractTags(data, summary);

        Job job = Job.builder()
                .source(sourceName())
                .externalId(id.isEmpty() ? title : id)
                .title(title)
                .company(company)
                .location(location)
                .postedAt(postedAt)
                .url(url)
                .tags(tags)
                .build();
        String content = extractContent(data, summary);
        return new FetchedJob(job, content);
    }

    private String extractLocation(Map<String, Object> detail, Map<String, Object> summary) {
        Object offices = detail.get("offices");
        List<?> officeList = offices instanceof List<?> list ? list : null;
        if (officeList == null || officeList.isEmpty()) {
            offices = summary.get("offices");
            List<?> summaryOffices = offices instanceof List<?> list ? list : null;
            if (summaryOffices == null || summaryOffices.isEmpty()) {
                Object location = detail.get("location");
                if (location instanceof Map<?, ?> locationMap) {
                    Object name = locationMap.get("name");
                    if (name != null) {
                        return name.toString();
                    }
                }
                return string(detail.getOrDefault("location", summary.get("location")));
            }
            officeList = summaryOffices;
        }
        Object office = officeList.get(0);
        if (office instanceof Map<?, ?> officeMap) {
            Object name = officeMap.get("name");
            if (name != null) {
                return name.toString();
            }
        }
        return string(detail.getOrDefault("location", summary.get("location")));
    }

    private Instant extractPostedAt(Map<String, Object> detail, Map<String, Object> summary) {
        Object updatedAt = detail.getOrDefault("updated_at", summary.get("updated_at"));
        String text = string(updatedAt);
        if (!text.isEmpty()) {
            try {
                return Instant.parse(text);
            } catch (DateTimeParseException ignored) {
            }
        }
        return Instant.now();
    }

    private Set<String> extractTags(Map<String, Object> detail, Map<String, Object> summary) {
        Set<String> tags = new HashSet<>();
        addTags(tags, detail.get("departments"));
        addTags(tags, summary.get("departments"));
        addTags(tags, detail.get("offices"));
        return tags;
    }

    private String extractContent(Map<String, Object> detail, Map<String, Object> summary) {
        String content = string(detail.get("content"));
        if (!content.isEmpty()) {
            return content;
        }
        content = string(detail.get("body"));
        if (!content.isEmpty()) {
            return content;
        }
        content = string(detail.get("description"));
        if (!content.isEmpty()) {
            return content;
        }
        content = string(summary.get("content"));
        if (!content.isEmpty()) {
            return content;
        }
        return string(summary.get("body"));
    }

    private void addTags(Set<String> tags, Object value) {
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Object name = map.get("name");
                    if (name != null) {
                        tags.add(name.toString());
                    }
                } else if (item != null) {
                    tags.add(item.toString());
                }
            }
        }
    }

    private String string(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}

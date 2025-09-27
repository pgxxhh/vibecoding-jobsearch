package com.vibe.jobs.sources;

import com.vibe.jobs.domain.Job;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fetch jobs from Workday "cxs" endpoints: {baseUrl}/wday/cxs/{tenant}/{site}/jobs
 */
public class WorkdaySourceClient implements SourceClient {

    private final String company;
    private final String baseUrl;
    private final String tenant;
    private final String site;
    private final WebClient client;

    public WorkdaySourceClient(String company, String baseUrl, String tenant, String site) {
        this.company = company;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.tenant = tenant;
        this.site = site;
        this.client = WebClient.builder()
                .baseUrl(this.baseUrl)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)")
                .build();
    }

    @Override
    public String sourceName() {
        return "workday:" + tenant;
    }

    @Override
    public List<FetchedJob> fetchPage(int page, int size) {
        int limit = Math.max(1, Math.min(size, 100));
        int offset = Math.max(page - 1, 0) * limit;

        Map<String, Object> response = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/wday/cxs/" + tenant + "/" + site + "/jobs")
                        .queryParam("offset", offset)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("jobPostings")) {
            return List.of();
        }

        List<Map<String, Object>> jobPostings = (List<Map<String, Object>>) response.get("jobPostings");
        if (jobPostings == null || jobPostings.isEmpty()) {
            return List.of();
        }

        return jobPostings.stream()
                .map(this::mapJob)
                .toList();
    }

    private FetchedJob mapJob(Map<String, Object> posting) {
        String jobId = stringValue(posting.get("jobPostingId"));
        String title = stringValue(posting.get("title"));
        String location = stringValue(posting.get("locationsText"));
        if (location.isEmpty()) {
            Object locations = posting.get("locations");
            if (locations instanceof List<?> locationList) {
                location = locationList.stream()
                        .map(this::stringValue)
                        .filter(value -> !value.isEmpty())
                        .findFirst()
                        .orElse("");
            }
        }
        String externalPath = stringValue(posting.get("externalPath"));

        String url = externalPath.isEmpty() ? baseUrl : baseUrl + externalPath;

        Instant postedAt = Instant.now();
        postedAt = parseInstant(posting.get("postedOn"), postedAt);
        postedAt = parseInstant(posting.get("postedOnTime"), postedAt);

        Set<String> tags = new HashSet<>();
        addTags(tags, posting.get("jobFamilies"));
        addTags(tags, posting.get("jobFamilyGroup"));
        addTags(tags, posting.get("jobFamily"));

        Job job = Job.builder()
                .source(sourceName())
                .externalId(jobId.isEmpty() ? title : jobId)
                .title(title)
                .company(company)
                .location(location)
                .url(url)
                .postedAt(postedAt)
                .tags(tags)
                .build();
        String content = extractContent(posting);
        return new FetchedJob(job, content);
    }

    private Instant parseInstant(Object value, Instant fallback) {
        if (value instanceof String text) {
            String trimmed = text.trim();
            if (!trimmed.isEmpty()) {
                try {
                    return Instant.parse(trimmed);
                } catch (DateTimeParseException ignored) {
                }
            }
        }
        if (value instanceof Number number) {
            return Instant.ofEpochMilli(number.longValue());
        }
        return fallback;
    }

    private void addTags(Set<String> tags, Object candidate) {
        if (candidate instanceof List<?> list) {
            list.forEach(item -> addTags(tags, item));
            return;
        }
        if (candidate instanceof Map<?, ?> map) {
            Object label = map.get("label");
            if (label != null) {
                addTags(tags, label);
            }
            Object value = map.get("value");
            if (value != null) {
                addTags(tags, value);
            }
            return;
        }
        String value = stringValue(candidate);
        if (!value.isEmpty()) {
            tags.add(value);
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String extractContent(Map<String, Object> posting) {
        String content = stringValue(posting.get("jobPostingDescription"));
        if (!content.isEmpty()) {
            return content;
        }
        content = stringValue(posting.get("description"));
        if (!content.isEmpty()) {
            return content;
        }
        content = stringValue(posting.get("jobDescription"));
        if (!content.isEmpty()) {
            return content;
        }
        Object mapping = posting.get("jobPostingText");
        if (mapping instanceof Map<?, ?> map) {
            Object longText = map.get("longDescription");
            if (longText != null) {
                content = stringValue(longText);
                if (!content.isEmpty()) {
                    return content;
                }
            }
        }
        return content;
    }

    private String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}

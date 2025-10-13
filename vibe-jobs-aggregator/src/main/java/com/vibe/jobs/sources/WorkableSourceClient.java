package com.vibe.jobs.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.jobs.domain.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Client for Workable public jobs API.
 * Endpoint: https://apply.workable.com/api/v3/accounts/{account}/jobs
 */
public class WorkableSourceClient implements SourceClient {

    private static final Logger log = LoggerFactory.getLogger(WorkableSourceClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final String company;
    private final String apiBase;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public WorkableSourceClient(String company, String baseUrl) {
        if (company == null || company.isBlank()) {
            throw new IllegalArgumentException("Workable company must be provided");
        }
        this.company = company.trim();
        if (baseUrl == null || baseUrl.isBlank()) {
            this.apiBase = "https://apply.workable.com/api/v3/accounts/" + this.company;
        } else {
            String normalized = baseUrl.trim();
            this.apiBase = normalized.endsWith("/jobs") ? normalized.substring(0, normalized.length() - 5) : normalized;
        }
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String sourceName() {
        return "workable:" + company;
    }

    @Override
    public List<FetchedJob> fetchPage(int page, int size) throws Exception {
        int limit = Math.max(1, Math.min(size, 50));
        int offset = Math.max(0, (Math.max(page, 1) - 1) * limit);
        String url = apiBase + "/jobs?limit=" + limit + "&offset=" + offset;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", "VibeCoding-JobAggregator/1.0")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status == 404) {
            return List.of();
        }
        if (status >= 400) {
            throw new IllegalStateException("Workable API error: " + status + " - " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode jobsNode = root.path("jobs");
        if (!jobsNode.isArray() || jobsNode.isEmpty()) {
            return List.of();
        }

        List<FetchedJob> jobs = new ArrayList<>();
        for (JsonNode jobNode : jobsNode) {
            FetchedJob job = mapJob(jobNode);
            if (job != null) {
                jobs.add(job);
            }
        }
        return jobs;
    }

    private FetchedJob mapJob(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String id = node.path("id").asText("");
        if (id.isBlank()) {
            id = node.path("shortcode").asText("");
        }
        if (id.isBlank()) {
            return null;
        }
        String title = node.path("title").asText("").trim();
        if (title.isEmpty()) {
            return null;
        }

        String url = node.path("url").asText("");
        if (url.isBlank()) {
            url = node.path("application_url").asText("");
        }

        Instant publishedAt = parseInstant(node.path("published_on").asText(null));
        if (publishedAt == null) {
            publishedAt = parseInstant(node.path("updated_at").asText(null));
        }
        if (publishedAt == null) {
            publishedAt = Instant.now();
        }

        String location = extractLocation(node.path("locations"));

        Set<String> tags = new HashSet<>();
        tags.add("workable");
        addTag(tags, node.path("department").asText(""));
        addTag(tags, node.path("function").asText(""));

        String content = node.path("description").asText("");
        if (content.isBlank()) {
            content = node.toString();
        }

        Job job = Job.builder()
                .source(sourceName())
                .externalId(id)
                .title(title)
                .company(company)
                .location(location)
                .postedAt(publishedAt)
                .url(url)
                .tags(tags)
                .build();
        return FetchedJob.of(job, content);
    }

    private void addTag(Set<String> tags, String value) {
        if (value == null) {
            return;
        }
        String trimmed = value.trim();
        if (!trimmed.isEmpty()) {
            tags.add(trimmed.toLowerCase(Locale.ROOT));
        }
    }

    private String extractLocation(JsonNode locations) {
        if (locations == null || locations.isNull()) {
            return "";
        }
        if (locations.isArray() && !locations.isEmpty()) {
            JsonNode first = locations.get(0);
            if (first != null && !first.isNull()) {
                List<String> parts = new ArrayList<>();
                addLocationPart(parts, first.get("city"));
                addLocationPart(parts, first.get("region"));
                addLocationPart(parts, first.get("country"));
                JsonNode remoteNode = first.get("remote");
                if (remoteNode != null && remoteNode.isBoolean() && remoteNode.asBoolean()) {
                    addLocationPart(parts, "Remote");
                }
                String combined = String.join(", ", parts);
                if (!combined.isBlank()) {
                    return combined;
                }
            }
        }
        return locations.toString();
    }

    private void addLocationPart(List<String> parts, JsonNode value) {
        if (value == null || value.isNull()) {
            return;
        }
        addLocationPart(parts, value.asText(null));
    }

    private void addLocationPart(List<String> parts, String value) {
        if (value == null) {
            return;
        }
        String trimmed = value.trim();
        if (!trimmed.isEmpty() && !parts.contains(trimmed)) {
            parts.add(trimmed);
        }
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
        } catch (DateTimeParseException ex) {
            try {
                return Instant.parse(value);
            } catch (DateTimeParseException ignored) {
                log.info("Unable to parse Workable timestamp: {}", value);
                return null;
            }
        }
    }
}


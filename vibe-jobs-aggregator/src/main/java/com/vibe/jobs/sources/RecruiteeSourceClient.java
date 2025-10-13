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
 * Client for Recruitee public offers API.
 * Endpoint: https://{company}.recruitee.com/api/offers
 */
public class RecruiteeSourceClient implements SourceClient {

    private static final Logger log = LoggerFactory.getLogger(RecruiteeSourceClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final String company;
    private final String apiBase;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public RecruiteeSourceClient(String company, String baseUrl) {
        if (company == null || company.isBlank()) {
            throw new IllegalArgumentException("Recruitee company must be provided");
        }
        this.company = company.trim();
        if (baseUrl == null || baseUrl.isBlank()) {
            this.apiBase = "https://" + this.company + ".recruitee.com/api";
        } else {
            String normalized = baseUrl.trim();
            this.apiBase = normalized.endsWith("/api") ? normalized : normalized + "/api";
        }
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String sourceName() {
        return "recruitee:" + company;
    }

    @Override
    public List<FetchedJob> fetchPage(int page, int size) throws Exception {
        int limit = Math.max(1, Math.min(size, 100));
        int offset = Math.max(0, (Math.max(page, 1) - 1) * limit);
        String url = apiBase + "/offers/?limit=" + limit + "&offset=" + offset;

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
            throw new IllegalStateException("Recruitee API error: " + status + " - " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode offers = root.path("offers");
        if (!offers.isArray() || offers.isEmpty()) {
            return List.of();
        }

        List<FetchedJob> jobs = new ArrayList<>();
        for (JsonNode offer : offers) {
            FetchedJob job = mapOffer(offer);
            if (job != null) {
                jobs.add(job);
            }
        }
        return jobs;
    }

    private FetchedJob mapOffer(JsonNode offer) {
        if (offer == null || offer.isNull()) {
            return null;
        }
        String id = offer.path("id").asText("");
        if (id.isBlank()) {
            return null;
        }
        String title = offer.path("title").asText("").trim();
        if (title.isEmpty()) {
            return null;
        }

        String url = offer.path("careers_url").asText("");
        if (url.isBlank()) {
            url = offer.path("apply_url").asText("");
        }

        Instant publishedAt = parseInstant(offer.path("published_at").asText(null));
        if (publishedAt == null) {
            publishedAt = parseInstant(offer.path("created_at").asText(null));
        }
        if (publishedAt == null) {
            publishedAt = Instant.now();
        }

        String location = extractLocation(offer.path("location"));
        if (location.isBlank()) {
            location = offer.path("country").asText("");
        }

        Set<String> tags = new HashSet<>();
        tags.add("recruitee");
        String department = offer.path("department").asText("");
        if (!department.isBlank()) {
            tags.add(department.toLowerCase(Locale.ROOT));
        }
        String category = offer.path("category").asText("");
        if (!category.isBlank()) {
            tags.add(category.toLowerCase(Locale.ROOT));
        }

        String content = offer.path("description").asText("");
        if (content.isBlank()) {
            content = offer.toString();
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

    private String extractLocation(JsonNode location) {
        if (location == null || location.isNull()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        addPart(parts, location.get("city"));
        addPart(parts, location.get("region"));
        addPart(parts, location.get("country"));
        return String.join(", ", parts);
    }

    private void addPart(List<String> parts, JsonNode node) {
        if (node == null || node.isNull()) {
            return;
        }
        String text = node.asText("").trim();
        if (!text.isEmpty() && !parts.contains(text)) {
            parts.add(text);
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
                log.info("Unable to parse Recruitee timestamp: {}", value);
                return null;
            }
        }
    }
}


package com.vibe.jobs.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.jobs.domain.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
 * Client for SmartRecruiters public postings API.
 * API reference: https://api.smartrecruiters.com/v1/companies/{company}/postings
 */
public class SmartRecruitersSourceClient implements SourceClient {

    private static final Logger log = LoggerFactory.getLogger(SmartRecruitersSourceClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final DateTimeFormatter ISO_WITHOUT_COLON = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    private final String company;
    private final String apiBase;
    private final String siteBase;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SmartRecruitersSourceClient(String company, String baseUrl) {
        if (company == null || company.isBlank()) {
            throw new IllegalArgumentException("SmartRecruiters company must be provided");
        }
        this.company = company.trim();
        String normalizedBase = normalizeBaseUrl(baseUrl);
        this.apiBase = normalizedBase;
        this.siteBase = "https://jobs.smartrecruiters.com/" + encodePathSegment(this.company);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String sourceName() {
        return "smartrecruiters:" + company;
    }

    @Override
    public List<FetchedJob> fetchPage(int page, int size) throws Exception {
        int limit = Math.max(1, Math.min(size, 100));
        int offset = Math.max(0, (Math.max(page, 1) - 1) * limit);
        String requestUrl = apiBase + "/postings?limit=" + limit + "&offset=" + offset;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
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
            throw new IllegalStateException("SmartRecruiters API error: " + status + " - " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode content = root.path("content");
        if (!content.isArray() || content.isEmpty()) {
            return List.of();
        }

        List<FetchedJob> jobs = new ArrayList<>();
        for (JsonNode node : content) {
            FetchedJob job = mapJob(node);
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

        String id = text(node, "id");
        if (id.isBlank()) {
            id = text(node, "uuid");
        }
        String title = text(node, "name");
        if (title.isBlank()) {
            return null;
        }

        String url = text(node, "applyUrl");
        if (url.isBlank()) {
            url = text(node, "postingUrl");
        }
        if (url.isBlank()) {
            url = siteBase + "/" + encodePathSegment(id + "-" + title.toLowerCase(Locale.ROOT).replace(' ', '-'));
        }

        Instant postedAt = parseInstant(text(node, "releasedDate"));
        if (postedAt == null) {
            postedAt = parseInstant(text(node, "createdOn"));
        }
        if (postedAt == null) {
            postedAt = Instant.now();
        }

        Set<String> tags = new HashSet<>();
        tags.add("smartrecruiters");
        addIfPresent(tags, node.path("department").path("label"));
        addIfPresent(tags, node.path("function"));
        addIfPresent(tags, node.path("industry"));

        String location = buildLocation(node.path("location"));
        if (location.isBlank()) {
            location = text(node, "locationCity");
        }

        String content = extractContent(node);

        Job job = Job.builder()
                .source(sourceName())
                .externalId(id.isBlank() ? encodePathSegment(title) : id)
                .title(title)
                .company(company)
                .location(location)
                .postedAt(postedAt)
                .url(url)
                .tags(tags)
                .build();

        return FetchedJob.of(job, content);
    }

    private String text(JsonNode node, String field) {
        if (node == null || field == null) {
            return "";
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return "";
        }
        return value.asText("").trim();
    }

    private void addIfPresent(Set<String> tags, JsonNode node) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isTextual()) {
            String value = node.asText().trim();
            if (!value.isEmpty()) {
                tags.add(value.toLowerCase(Locale.ROOT));
            }
            return;
        }
        JsonNode label = node.get("label");
        if (label != null && label.isTextual()) {
            String value = label.asText().trim();
            if (!value.isEmpty()) {
                tags.add(value.toLowerCase(Locale.ROOT));
            }
        }
    }

    private String buildLocation(JsonNode locationNode) {
        if (locationNode == null || locationNode.isNull()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        addLocationPart(parts, locationNode.get("city"));
        addLocationPart(parts, locationNode.get("region"));
        addLocationPart(parts, locationNode.get("country"));
        addLocationPart(parts, locationNode.get("address"));
        String formatted = String.join(", ", parts);
        if (!formatted.isBlank()) {
            return formatted;
        }
        JsonNode formattedNode = locationNode.get("formattedAddress");
        if (formattedNode != null && formattedNode.isTextual()) {
            return formattedNode.asText().trim();
        }
        return "";
    }

    private void addLocationPart(List<String> parts, JsonNode value) {
        if (value == null || value.isNull()) {
            return;
        }
        String text = value.asText().trim();
        if (!text.isEmpty() && !parts.contains(text)) {
            parts.add(text);
        }
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            if (trimmed.matches(".*[+-]\\d{4}")) {
                return OffsetDateTime.parse(trimmed, ISO_WITHOUT_COLON).toInstant();
            }
            return OffsetDateTime.parse(trimmed, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
        } catch (DateTimeParseException ignored) {
            try {
                return Instant.parse(trimmed);
            } catch (DateTimeParseException ignored2) {
                log.info("Unable to parse SmartRecruiters timestamp: {}", trimmed);
                return null;
            }
        }
    }

    private String extractContent(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        JsonNode jobAd = node.get("jobAd");
        if (jobAd == null || jobAd.isNull()) {
            return "";
        }
        JsonNode sections = jobAd.get("sections");
        if (sections == null || sections.isNull()) {
            return jobAd.toString();
        }
        StringBuilder sb = new StringBuilder();
        sections.fields().forEachRemaining(entry -> {
            JsonNode textNode = entry.getValue().get("text");
            if (textNode != null && textNode.isTextual()) {
                if (sb.length() > 0) {
                    sb.append("\n\n");
                }
                sb.append(textNode.asText());
            }
        });
        if (sb.length() == 0) {
            return sections.toString();
        }
        return sb.toString();
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized;
        if (baseUrl == null || baseUrl.isBlank()) {
            normalized = "https://api.smartrecruiters.com/v1/companies/" + encodePathSegment(company);
        } else {
            normalized = baseUrl.trim();
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!normalized.toLowerCase(Locale.ROOT).contains("/companies/")) {
            normalized = normalized + "/companies/" + encodePathSegment(company);
        }
        if (normalized.toLowerCase(Locale.ROOT).endsWith("/postings")) {
            return normalized.substring(0, normalized.length() - "/postings".length());
        }
        return normalized;
    }

    private String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}


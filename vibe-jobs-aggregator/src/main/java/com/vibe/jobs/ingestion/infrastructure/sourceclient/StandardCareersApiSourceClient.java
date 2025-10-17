package com.vibe.jobs.ingestion.infrastructure.sourceclient;

import com.vibe.jobs.jobposting.domain.Job;
import org.springframework.web.reactive.function.client.WebClient;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Fetch jobs from a standardized Careers API (e.g., Microsoft, Google, Amazon).
 * Example endpoint: https://company.com/api/careers/jobs
 */
public class StandardCareersApiSourceClient implements SourceClient {
    private final String company;
    private final String apiUrl;
    private final String jobsPath;
    private final WebClient client;

    public StandardCareersApiSourceClient(String company, String apiUrl) {
        this(company, apiUrl, "/jobs");
    }

    public StandardCareersApiSourceClient(String company, String apiUrl, String jobsPath) {
        this.company = company;
        this.apiUrl = trimTrailingSlash(apiUrl);
        this.jobsPath = normalizePath(jobsPath);
        String origin = resolveOrigin(this.apiUrl);
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(this.apiUrl)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)")
                .defaultHeader("Accept-Language", "en-US,en;q=0.9");
        if (!origin.isEmpty()) {
            builder.defaultHeader("Referer", origin + "/")
                    .defaultHeader("Origin", origin);
        }
        this.client = builder.build();
    }

    private static String resolveOrigin(String url) {
        try {
            URI uri = URI.create(url);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return "";
            }
            StringBuilder origin = new StringBuilder();
            origin.append(uri.getScheme()).append("://").append(uri.getHost());
            if (uri.getPort() != -1) {
                origin.append(":").append(uri.getPort());
            }
            return origin.toString();
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank() || "/".equals(path.trim())) {
            return "";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    @Override
    public String sourceName() {
        return "careersapi:" + company;
    }

    @Override
    public List<FetchedJob> fetchPage(int page, int size) throws Exception {
        Map<String, Object> response = client.get()
                .uri(uriBuilder -> {
                    if (!jobsPath.isEmpty()) {
                        uriBuilder.path(jobsPath);
                    }
                    return uriBuilder
                            .queryParam("page", page)
                            .queryParam("size", size)
                            .build();
                })
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("jobs")) return List.of();
        List<Map<String, Object>> jobs = (List<Map<String, Object>>) response.get("jobs");

        return jobs.stream().map(j -> {
            String externalId = String.valueOf(j.get("id"));
            String title = (String) j.get("title");
            String location = (String) j.getOrDefault("location", null);
            String url = (String) j.getOrDefault("applyUrl", null); // URL to job description
            Instant postedAt = Instant.now();
            if (j.containsKey("postedAt")) {
                try {
                    postedAt = Instant.parse((String) j.get("postedAt"));
                } catch (Exception ignored) {}
            }
            Set<String> tags = new HashSet<>();
            if (j.containsKey("tags") && j.get("tags") instanceof List<?> tagList) {
                for (Object tag : tagList) {
                    tags.add(String.valueOf(tag));
                }
            }
            Job job = Job.builder()
                    .source(sourceName())
                    .externalId(externalId)
                    .title(title)
                    .company(company)
                    .location(location)
                    .postedAt(postedAt)
                    .url(url)
                    .tags(tags)
                    .build();
            String content = extractContent(j);
            return new FetchedJob(job, content);
        }).collect(Collectors.toList());
    }

    private String extractContent(Map<String, Object> payload) {
        if (payload == null) {
            return "";
        }
        Object description = payload.get("description");
        if (description instanceof String s && !s.isBlank()) {
            return s;
        }
        description = payload.get("content");
        if (description instanceof String s2 && !s2.isBlank()) {
            return s2;
        }
        description = payload.get("body");
        if (description instanceof String s3 && !s3.isBlank()) {
            return s3;
        }
        return "";
    }
}

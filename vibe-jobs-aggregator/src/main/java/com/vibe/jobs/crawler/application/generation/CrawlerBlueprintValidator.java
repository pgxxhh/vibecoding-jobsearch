package com.vibe.jobs.crawler.application.generation;

import com.vibe.jobs.crawler.domain.ParserField;
import com.vibe.jobs.crawler.domain.ParserProfile;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CrawlerBlueprintValidator {

    public ValidationResult validate(ParserProfile profile, String html) {
        List<Map<String, Object>> samples = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<ParserProfile.ParsedJob> jobs;
        try {
            jobs = profile.parse(html);
        } catch (RuntimeException ex) {
            warnings.add("Parser execution failed: " + ex.getMessage());
            Map<String, Object> metrics = Map.of(
                    "jobCount", 0,
                    "parsedAt", Instant.now().toString()
            );
            return new ValidationResult(false, metrics, samples, List.copyOf(warnings));
        }

        for (int i = 0; i < Math.min(10, jobs.size()); i++) {
            ParserProfile.ParsedJob job = jobs.get(i);
            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("externalId", job.externalId());
            sample.put("title", job.title());
            sample.put("company", job.company());
            sample.put("location", job.location());
            sample.put("url", job.url());
            sample.put("level", job.level());
            sample.put("postedAt", job.postedAt() == null ? null : job.postedAt().toString());
            sample.put("description", job.description());
            sample.put("tags", job.tags());
            samples.add(sample);
        }

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("jobCount", jobs.size());
        metrics.put("parsedAt", Instant.now().toString());
        Map<String, Object> coverage = evaluateFieldCoverage(profile, html, warnings);
        if (!coverage.isEmpty()) {
            metrics.put("fieldCoverage", coverage);
        }

        boolean success = !jobs.isEmpty();
        if (!success) {
            warnings.add("No jobs parsed with generated selectors");
        }

        return new ValidationResult(success, metrics, samples, List.copyOf(warnings));
    }

    private Map<String, Object> evaluateFieldCoverage(ParserProfile profile, String html, List<String> warnings) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (profile == null || profile.listSelector() == null || profile.listSelector().isBlank()) {
            warnings.add("Parser list selector is empty; unable to evaluate field coverage.");
            return result;
        }

        Document document = Jsoup.parse(html == null ? "" : html);
        Elements elements = document.select(profile.listSelector());
        if (elements.isEmpty()) {
            warnings.add("List selector '" + profile.listSelector() + "' did not match any elements.");
        }

        Map<String, FieldCoverage> coverageMap = new LinkedHashMap<>();
        for (Map.Entry<String, ParserField> entry : profile.fields().entrySet()) {
            coverageMap.put(entry.getKey(), new FieldCoverage());
        }

        int inspected = 0;
        for (Element element : elements) {
            for (Map.Entry<String, ParserField> entry : profile.fields().entrySet()) {
                FieldCoverage coverage = coverageMap.get(entry.getKey());
                if (coverage == null) {
                    continue;
                }
                try {
                    Object value = entry.getValue().extract(element);
                    if (isEmptyValue(value)) {
                        coverage.recordFailure(element, "empty");
                    } else {
                        coverage.recordSuccess();
                    }
                } catch (RuntimeException ex) {
                    coverage.recordFailure(element, "exception: " + ex.getMessage());
                }
            }
            inspected++;
            if (inspected >= 25) {
                break;
            }
        }

        for (Map.Entry<String, ParserField> entry : profile.fields().entrySet()) {
            FieldCoverage coverage = coverageMap.get(entry.getKey());
            if (coverage == null) {
                continue;
            }
            Map<String, Object> info = coverage.toMetrics(elements.size());
            info.put("required", entry.getValue().required());
            result.put(entry.getKey(), info);
            if (entry.getValue().required()) {
                if (coverage.successCount == 0) {
                    warnings.add("Required field '" + entry.getKey() + "' missing in detected listings" + coverage.describeFirstFailure());
                } else if (coverage.failureCount > 0) {
                    warnings.add("Required field '" + entry.getKey() + "' missing in " + coverage.failureCount + " of " + elements.size() + " detected nodes" + coverage.describeFirstFailure());
                }
            }
        }

        return result;
    }

    private boolean isEmptyValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String str) {
            return str.trim().isEmpty();
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        if (value.getClass().isArray()) {
            return Array.getLength(value) == 0;
        }
        return false;
    }

    private String describeElement(Element element) {
        if (element == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(element.tagName());
        if (!element.id().isBlank()) {
            builder.append("#").append(element.id());
        }
        if (!element.className().isBlank()) {
            builder.append(".").append(element.className().trim().replaceAll("\\s+", "."));
        }
        String text = element.text() == null ? "" : element.text().trim().replaceAll("\\s+", " ");
        if (text.length() > 80) {
            text = text.substring(0, 77) + "...";
        }
        if (!text.isBlank()) {
            builder.append(" -> \"").append(text).append("\"");
        }
        return builder.toString();
    }

    private class FieldCoverage {
        private int successCount;
        private int failureCount;
        private String firstFailureSnippet;
        private String firstFailureReason;

        void recordSuccess() {
            successCount++;
        }

        void recordFailure(Element element, String reason) {
            failureCount++;
            if (firstFailureSnippet == null) {
                firstFailureSnippet = describeElement(element);
                firstFailureReason = reason;
            }
        }

        Map<String, Object> toMetrics(int total) {
            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("successCount", successCount);
            metrics.put("failureCount", failureCount);
            metrics.put("inspected", total);
            if (firstFailureSnippet != null && !firstFailureSnippet.isBlank()) {
                metrics.put("firstFailure", firstFailureSnippet);
            }
            if (firstFailureReason != null) {
                metrics.put("firstFailureReason", firstFailureReason);
            }
            return metrics;
        }

        String describeFirstFailure() {
            if (firstFailureSnippet == null || firstFailureSnippet.isBlank()) {
                return "";
            }
            String reason = firstFailureReason == null ? "" : " [" + firstFailureReason + "]";
            return " (example: " + firstFailureSnippet + reason + ")";
        }
    }

    public record ValidationResult(boolean success,
                                   Map<String, Object> metrics,
                                   List<Map<String, Object>> samples,
                                   List<String> warnings) {
    }
}

package com.vibe.jobs.crawler.application.generation;

import com.vibe.jobs.crawler.domain.ParserProfile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CrawlerBlueprintValidator {

    public ValidationResult validate(ParserProfile profile, String html) {
        List<ParserProfile.ParsedJob> jobs = profile.parse(html);
        List<Map<String, Object>> samples = new ArrayList<>();
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
        Map<String, Object> metrics = Map.of(
                "jobCount", jobs.size(),
                "parsedAt", Instant.now().toString()
        );
        boolean success = !jobs.isEmpty();
        List<String> warnings = success ? List.of() : List.of("No jobs parsed with generated selectors");
        return new ValidationResult(success, metrics, samples, warnings);
    }

    public record ValidationResult(boolean success,
                                   Map<String, Object> metrics,
                                   List<Map<String, Object>> samples,
                                   List<String> warnings) {
    }
}

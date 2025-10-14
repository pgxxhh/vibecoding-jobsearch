
package com.vibe.jobs.web;

import com.vibe.jobs.repo.JobRepository;
import com.vibe.jobs.service.JobDetailService;
import com.vibe.jobs.web.dto.JobDetailResponse;
import com.vibe.jobs.web.dto.JobsResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/jobs")
@CrossOrigin(origins = "*")
public class JobController {
    private final JobRepository repo;
    private final JobDetailService jobDetailService;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    public JobController(JobRepository repo, JobDetailService jobDetailService) {
        this.repo = repo;
        this.jobDetailService = jobDetailService;
    }

    @GetMapping
    public JobsResponse list(@RequestParam(value = "q", required = false) String q,
                             @RequestParam(value = "company", required = false) String company,
                             @RequestParam(value = "location", required = false) String location,
                             @RequestParam(value = "level", required = false) String level,
                             @RequestParam(value = "datePosted", required = false) Integer datePosted,
                             @RequestParam(value = "searchDetail", defaultValue = "false") boolean searchDetail,
                             @RequestParam(value = "cursor", required = false) String cursor,
                             @RequestParam(value = "size", defaultValue = "10") int size) {
        if (size < 1) size = DEFAULT_SIZE;
        size = Math.min(size, MAX_SIZE);

        var cursorPosition = decodeCursor(cursor);

        Instant postedAfter = null;
        if (datePosted != null && datePosted > 0) {
            int days = Math.max(datePosted, 1);
            LocalDate referenceDate = LocalDate.now(ZoneOffset.UTC).minusDays(days - 1L);
            postedAfter = referenceDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        }

        Pageable pageable = PageRequest.of(
                0,
                size + 1,
                Sort.by(Sort.Direction.DESC, "postedAt", "id")
        );

        String normalizedQuery = emptyToNull(q);
        boolean detailEnabled = searchDetail && normalizedQuery != null;

        var jobs = new ArrayList<>(repo.searchAfter(
                normalizedQuery,
                emptyToNull(company),
                emptyToNull(location),
                emptyToNull(level),
                postedAfter,
                cursorPosition != null ? cursorPosition.postedAt() : null,
                cursorPosition != null ? cursorPosition.id() : null,
                detailEnabled,
                pageable
        ));

        boolean hasMore = jobs.size() > size;
        if (hasMore) {
            jobs = new ArrayList<>(jobs.subList(0, size));
        }

        String nextCursor = null;
        if (hasMore && !jobs.isEmpty()) {
            var last = jobs.get(jobs.size() - 1);
            if (last.getPostedAt() != null && last.getId() != null) {
                nextCursor = encodeCursor(last.getPostedAt(), last.getId());
            }
        }

        var detailMatches = detailEnabled ? findDetailMatches(jobs, normalizedQuery) : java.util.Collections.<Long>emptySet();

        var detailByJobId = jobDetailService.findByJobIds(jobs.stream()
                .map(com.vibe.jobs.domain.Job::getId)
                .collect(Collectors.toSet()));

        var items = jobs.stream()
                .map(job -> com.vibe.jobs.web.JobMapper.toDto(job,
                        detailMatches.contains(job.getId()),
                        detailByJobId.get(job.getId())))
                .collect(Collectors.toList());
        long total = repo.countSearch(normalizedQuery, emptyToNull(company), emptyToNull(location), emptyToNull(level), postedAfter, detailEnabled);
        return new JobsResponse(items, total, nextCursor, hasMore, size);
    }

    @GetMapping("/{id}/detail")
    public JobDetailResponse detail(@PathVariable Long id) {
        var job = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        var detail = jobDetailService.findByJob(job).orElse(null);
        String content = detail != null ? detail.getContent() : "";
        
        // 使用JobEnrichmentExtractor来提取enrichment数据
        String summary = detail != null ? JobEnrichmentExtractor.summary(detail).orElse(null) : null;
        var skills = detail != null ? sanitizeList(JobEnrichmentExtractor.skills(detail)) : java.util.List.<String>of();
        var highlights = detail != null ? sanitizeList(JobEnrichmentExtractor.highlights(detail)) : java.util.List.<String>of();
        String structuredData = detail != null ? JobEnrichmentExtractor.structured(detail).orElse(null) : null;
        var enrichments = detail != null ? JobEnrichmentExtractor.enrichments(detail) : java.util.Map.<String, Object>of();
        var status = detail != null ? JobEnrichmentExtractor.status(detail).orElse(java.util.Map.of()) : java.util.Map.<String, Object>of();
        
        return new JobDetailResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getLocation(),
                job.getPostedAt(),
                content,
                enrichments,
                status,
                summary,
                skills,
                highlights,
                structuredData
        );
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private CursorPosition decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            var decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            var parts = decoded.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid cursor format");
            }
            long postedAtMillis = Long.parseLong(parts[0]);
            long id = Long.parseLong(parts[1]);
            return new CursorPosition(Instant.ofEpochMilli(postedAtMillis), id);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor");
        }
    }

    private String encodeCursor(Instant postedAt, Long id) {
        String payload = postedAt.toEpochMilli() + ":" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private java.util.Set<Long> findDetailMatches(java.util.List<com.vibe.jobs.domain.Job> jobs, String query) {
        var jobIds = jobs.stream()
                .map(com.vibe.jobs.domain.Job::getId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toList());
        if (jobIds.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        return jobDetailService.findMatchingJobIds(jobIds, query);
    }

    private record CursorPosition(Instant postedAt, long id) {}

    private java.util.List<String> sanitizeList(java.util.List<String> values) {
        if (values == null || values.isEmpty()) {
            return java.util.List.of();
        }
        return values.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toList());
    }
}

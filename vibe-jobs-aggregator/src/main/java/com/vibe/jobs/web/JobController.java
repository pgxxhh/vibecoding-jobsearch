
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
                             @RequestParam(value = "cursor", required = false) String cursor,
                             @RequestParam(value = "size", defaultValue = "10") int size) {
        if (size < 1) size = DEFAULT_SIZE;
        size = Math.min(size, MAX_SIZE);

        var cursorPosition = decodeCursor(cursor);

        Pageable pageable = PageRequest.of(
                0,
                size + 1,
                Sort.by(Sort.Direction.DESC, "postedAt", "id")
        );

        var jobs = new ArrayList<>(repo.searchAfter(
                emptyToNull(q),
                emptyToNull(company),
                emptyToNull(location),
                emptyToNull(level),
                cursorPosition != null ? cursorPosition.postedAt() : null,
                cursorPosition != null ? cursorPosition.id() : null,
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

        var items = jobs.stream().map(com.vibe.jobs.web.JobMapper::toDto).collect(Collectors.toList());
        long total = repo.countSearch(emptyToNull(q), emptyToNull(company), emptyToNull(location), emptyToNull(level));
        return new JobsResponse(items, total, nextCursor, hasMore, size);
    }

    @GetMapping("/{id}/detail")
    public JobDetailResponse detail(@PathVariable Long id) {
        var job = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        String content = jobDetailService.findByJob(job)
                .map(com.vibe.jobs.domain.JobDetail::getContent)
                .orElse("");
        return new JobDetailResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getLocation(),
                job.getPostedAt(),
                content
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

    private record CursorPosition(Instant postedAt, long id) {}
}

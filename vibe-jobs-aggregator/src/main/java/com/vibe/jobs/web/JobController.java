
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

import java.util.stream.Collectors;

@RestController
@RequestMapping("/jobs")
@CrossOrigin(origins = "*")
public class JobController {
    private final JobRepository repo;
    private final JobDetailService jobDetailService;

    public JobController(JobRepository repo, JobDetailService jobDetailService) {
        this.repo = repo;
        this.jobDetailService = jobDetailService;
    }

    @GetMapping
    public JobsResponse list(@RequestParam(value = "q", required = false) String q,
                             @RequestParam(value = "company", required = false) String company,
                             @RequestParam(value = "location", required = false) String location,
                             @RequestParam(value = "level", required = false) String level,
                             @RequestParam(value = "page", defaultValue = "1") int page,
                             @RequestParam(value = "size", defaultValue = "10") int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "postedAt", "id"));
        var p = repo.search(emptyToNull(q), emptyToNull(company), emptyToNull(location), emptyToNull(level), pageable);
        var items = p.getContent().stream().map(com.vibe.jobs.web.JobMapper::toDto).collect(Collectors.toList());
        return new JobsResponse(items, p.getTotalElements(), page, size);
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
}

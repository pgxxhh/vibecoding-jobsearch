
package com.vibe.jobs.web.dto;
import java.util.List;
public record JobsResponse(
        List<JobDto> items,
        long total,
        int page,
        int size
) {}

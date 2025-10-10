
package com.vibe.jobs.web.dto;
import java.time.Instant;
import java.util.List;
public record JobDto(
        String id,
        String title,
        String company,
        String location,
        String level,
        Instant postedAt,
        List<String> tags,
        String url,
        boolean detailMatch
) {}

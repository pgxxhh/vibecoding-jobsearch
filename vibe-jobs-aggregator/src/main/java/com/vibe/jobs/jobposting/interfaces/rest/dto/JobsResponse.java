
package com.vibe.jobs.jobposting.interfaces.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public record JobsResponse(
        List<JobDto> items,
        @JsonInclude(JsonInclude.Include.ALWAYS)
        Long total,
        String nextCursor,
        boolean hasMore,
        int size
) {}

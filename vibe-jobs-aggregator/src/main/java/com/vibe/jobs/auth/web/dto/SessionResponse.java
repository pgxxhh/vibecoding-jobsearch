package com.vibe.jobs.auth.web.dto;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
        UUID userId,
        String email,
        Instant sessionExpiresAt
) {}

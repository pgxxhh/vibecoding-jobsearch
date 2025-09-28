package com.vibe.jobs.auth.web.dto;

import java.time.Instant;
import java.util.UUID;

public record VerifyCodeResponse(
        UUID userId,
        String email,
        String sessionToken,
        Instant sessionExpiresAt
) {}

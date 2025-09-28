package com.vibe.jobs.auth.web.dto;

import java.time.Instant;
import java.util.UUID;

public record SendCodeResponse(
        UUID challengeId,
        String maskedEmail,
        Instant expiresAt,
        Instant resendAvailableAt,
        String debugCode
) {}

package com.vibe.jobs.auth.interfaces.dto;

import java.time.Instant;
import java.util.UUID;

public record SendCodeResponse(
        UUID challengeId,
        String maskedEmail,
        Instant expiresAt,
        Instant resendAvailableAt
) {}

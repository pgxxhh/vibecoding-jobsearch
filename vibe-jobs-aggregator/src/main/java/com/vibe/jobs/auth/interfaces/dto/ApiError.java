package com.vibe.jobs.auth.interfaces.dto;

import java.time.Instant;

public record ApiError(String code, String message, Instant timestamp) {
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, Instant.now());
    }
}

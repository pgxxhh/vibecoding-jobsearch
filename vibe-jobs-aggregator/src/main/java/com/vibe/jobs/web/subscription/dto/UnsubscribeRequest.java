package com.vibe.jobs.web.subscription.dto;

import jakarta.validation.constraints.NotBlank;

public record UnsubscribeRequest(@NotBlank String token) {
}

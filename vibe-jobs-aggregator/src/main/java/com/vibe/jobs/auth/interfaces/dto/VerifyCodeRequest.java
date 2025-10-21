package com.vibe.jobs.auth.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyCodeRequest(
        @NotBlank(message = "Code is required")
        @Pattern(regexp = "\\d{6}", message = "Code must be 6 digits")
        String code
) {}

package com.vibe.jobs.web.session;

import java.util.UUID;

public record UserPrincipal(UUID userId, String email) {}

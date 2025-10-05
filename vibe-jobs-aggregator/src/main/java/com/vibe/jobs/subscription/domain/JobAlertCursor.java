package com.vibe.jobs.subscription.domain;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public record JobAlertCursor(Instant postedAt, long jobId) {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    public String encode() {
        String payload = postedAt.toEpochMilli() + ":" + jobId;
        return ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public static JobAlertCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String decoded = new String(DECODER.decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid cursor format");
            }
            long postedAtMillis = Long.parseLong(parts[0]);
            long id = Long.parseLong(parts[1]);
            return new JobAlertCursor(Instant.ofEpochMilli(postedAtMillis), id);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid cursor", ex);
        }
    }

    public static JobAlertCursor fromInstant(Instant instant) {
        return new JobAlertCursor(instant, Long.MAX_VALUE);
    }
}

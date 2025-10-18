package com.vibe.jobs.jobposting.domain;

public final class JobDetailEnrichmentStatus {

    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String RETRY_SCHEDULED = "RETRY_SCHEDULED";
    public static final String RETRYING = "RETRYING";

    private JobDetailEnrichmentStatus() {
    }
}

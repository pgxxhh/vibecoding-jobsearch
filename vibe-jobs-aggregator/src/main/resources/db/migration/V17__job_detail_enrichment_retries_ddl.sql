ALTER TABLE job_detail_enrichments
    ADD COLUMN IF NOT EXISTS status_state VARCHAR(32) NULL AFTER metadata_json,
    ADD COLUMN IF NOT EXISTS retry_count INT NOT NULL DEFAULT 0 AFTER status_state,
    ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMP NULL AFTER retry_count,
    ADD COLUMN IF NOT EXISTS last_attempt_at TIMESTAMP NULL AFTER next_retry_at,
    ADD COLUMN IF NOT EXISTS max_attempts INT NULL AFTER last_attempt_at;

CREATE INDEX IF NOT EXISTS idx_job_detail_enrichments_status_retry
    ON job_detail_enrichments (enrichment_key, status_state, next_retry_at);

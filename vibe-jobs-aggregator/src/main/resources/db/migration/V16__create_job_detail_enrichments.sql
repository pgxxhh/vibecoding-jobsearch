CREATE TABLE IF NOT EXISTS job_detail_enrichments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    job_detail_id BIGINT NOT NULL,
    enrichment_key VARCHAR(64) NOT NULL,
    value_json LONGTEXT NULL,
    source_fingerprint VARCHAR(128) NULL,
    provider VARCHAR(128) NULL,
    confidence DECIMAL(5,4) NULL,
    metadata_json LONGTEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT pk_job_detail_enrichments PRIMARY KEY (id),
    CONSTRAINT uk_job_detail_enrichments UNIQUE (job_detail_id, enrichment_key),
    CONSTRAINT fk_job_detail_enrichments_job_detail FOREIGN KEY (job_detail_id) REFERENCES job_details (id)
) ENGINE=InnoDB DEFAULT CHARSET = utf8mb4;

ALTER TABLE job_details
    ADD COLUMN IF NOT EXISTS content_version BIGINT NOT NULL DEFAULT 0 AFTER structured_data;

DROP TABLE IF EXISTS job_detail_skills;
DROP TABLE IF EXISTS job_detail_highlights;

ALTER TABLE job_details
    ADD COLUMN summary LONGTEXT NULL AFTER content_text;

ALTER TABLE job_details
    ADD COLUMN structured_data LONGTEXT NULL AFTER summary;

CREATE TABLE IF NOT EXISTS job_detail_skills (
    job_detail_id BIGINT NOT NULL,
    list_order INT NOT NULL,
    skill VARCHAR(255) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT pk_job_detail_skills PRIMARY KEY (job_detail_id, list_order),
    CONSTRAINT fk_job_detail_skills_job_detail FOREIGN KEY (job_detail_id) REFERENCES job_details (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_job_detail_skills_job_detail_id ON job_detail_skills (job_detail_id);

CREATE TABLE IF NOT EXISTS job_detail_highlights (
    job_detail_id BIGINT NOT NULL,
    list_order INT NOT NULL,
    highlight VARCHAR(512) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT pk_job_detail_highlights PRIMARY KEY (job_detail_id, list_order),
    CONSTRAINT fk_job_detail_highlights_job_detail FOREIGN KEY (job_detail_id) REFERENCES job_details (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_job_detail_highlights_job_detail_id ON job_detail_highlights (job_detail_id);

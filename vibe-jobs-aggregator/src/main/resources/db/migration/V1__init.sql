-- Baseline schema for migrating from the legacy H2 database to MySQL
-- Tables align with the existing JPA entities so Flyway can validate future changes.

CREATE TABLE IF NOT EXISTS jobs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source VARCHAR(255) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    company VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    level VARCHAR(255),
    posted_at DATETIME(6),
    url VARCHAR(1024),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    checksum VARCHAR(64),
    PRIMARY KEY (id),
    UNIQUE KEY idx_jobs_source_extid (source, external_id),
    KEY idx_jobs_title (title),
    KEY idx_jobs_company (company),
    KEY idx_jobs_location (location)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS job_tags (
    job_id BIGINT NOT NULL,
    tag VARCHAR(255) NOT NULL,
    PRIMARY KEY (job_id, tag),
    KEY idx_job_tags_tag (tag),
    CONSTRAINT fk_job_tags_job FOREIGN KEY (job_id)
        REFERENCES jobs (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS job_details (
    id BIGINT NOT NULL AUTO_INCREMENT,
    job_id BIGINT NOT NULL,
    content LONGTEXT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY idx_job_details_job_id (job_id),
    CONSTRAINT fk_job_details_job FOREIGN KEY (job_id)
        REFERENCES jobs (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS auth_user (
    id BINARY(16) NOT NULL,
    email VARCHAR(320) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    last_login_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY idx_auth_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS auth_login_challenge (
    id BINARY(16) NOT NULL,
    email VARCHAR(320) NOT NULL,
    code_hash VARCHAR(128) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    last_sent_at DATETIME(6) NOT NULL,
    verified BIT(1) NOT NULL DEFAULT b'0',
    attempts INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_auth_login_challenge_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS auth_session (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY idx_auth_session_token (token_hash),
    KEY idx_auth_session_user (user_id),
    CONSTRAINT fk_auth_session_user FOREIGN KEY (user_id)
        REFERENCES auth_user (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Manual database setup script
-- Run this directly on your MySQL database to create all required tables

-- Create all required tables for the application

-- V1: Basic job tables
CREATE TABLE IF NOT EXISTS jobs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(500) NOT NULL,
    company VARCHAR(200) NOT NULL,
    location VARCHAR(300),
    description TEXT,
    posted_at TIMESTAMP NOT NULL,
    url VARCHAR(1000),
    level VARCHAR(50),
    salary_min DECIMAL(15,2),
    salary_max DECIMAL(15,2),
    salary_currency VARCHAR(3),
    remote_ok BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_company (company),
    INDEX idx_location (location),
    INDEX idx_posted_at (posted_at),
    INDEX idx_level (level),
    INDEX idx_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS job_tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_id BIGINT NOT NULL,
    tag VARCHAR(100) NOT NULL,
    FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
    UNIQUE KEY uk_job_tag (job_id, tag),
    INDEX idx_tag (tag)
);

-- V3: Add performance indexes
CREATE INDEX IF NOT EXISTS idx_jobs_posted_at_id_desc ON jobs (posted_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_jobs_company_posted_at ON jobs ((LOWER(company)), posted_at);
CREATE INDEX IF NOT EXISTS idx_jobs_location_posted_at ON jobs ((LOWER(location)), posted_at);
CREATE INDEX IF NOT EXISTS idx_jobs_level ON jobs (level);
CREATE INDEX IF NOT EXISTS idx_job_tags_tag ON job_tags (tag);
CREATE INDEX IF NOT EXISTS idx_job_tags_job_tag ON job_tags (job_id, tag);

-- V10: Job data source tables
CREATE TABLE IF NOT EXISTS job_data_source (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(128) NOT NULL UNIQUE,
    type VARCHAR(128) NOT NULL,
    enabled BIT NOT NULL DEFAULT 1,
    run_on_startup BIT NOT NULL DEFAULT 1,
    require_override BIT NOT NULL DEFAULT 0,
    flow VARCHAR(16) NOT NULL DEFAULT 'UNLIMITED',
    base_options TEXT
);

CREATE TABLE IF NOT EXISTS job_data_source_company (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reference VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    slug VARCHAR(255),
    enabled BIT NOT NULL DEFAULT 1,
    placeholder_overrides TEXT,
    override_options TEXT,
    data_source_code VARCHAR(128) NOT NULL,
    INDEX idx_job_data_source_company_code (data_source_code),
    CONSTRAINT fk_job_data_source_company_code FOREIGN KEY (data_source_code) REFERENCES job_data_source (code) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS job_data_source_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    quota_limit INT NOT NULL,
    tags TEXT,
    facets TEXT,
    data_source_code VARCHAR(128) NOT NULL,
    INDEX idx_job_data_source_category_code (data_source_code),
    CONSTRAINT fk_job_data_source_category_code FOREIGN KEY (data_source_code) REFERENCES job_data_source (code) ON DELETE CASCADE
);

-- V10: Crawler tables
CREATE TABLE IF NOT EXISTS crawler_blueprint (
    code VARCHAR(128) PRIMARY KEY,
    name VARCHAR(256) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    entry_url VARCHAR(2048),
    concurrency_limit INT NOT NULL DEFAULT 1,
    config_json LONGTEXT,
    parser_template_code VARCHAR(128),
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS crawler_parser_template (
    code VARCHAR(128) PRIMARY KEY,
    description VARCHAR(256),
    config_json LONGTEXT,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS crawler_run_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    run_id VARCHAR(128) UNIQUE,
    blueprint_code VARCHAR(128),
    data_source_code VARCHAR(128),
    company VARCHAR(128),
    page_index INT,
    job_count INT,
    duration_ms BIGINT,
    success TINYINT(1) DEFAULT 1,
    error VARCHAR(1024),
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    INDEX idx_run_log_blueprint (blueprint_code),
    INDEX idx_run_log_started (started_at)
);

CREATE TABLE IF NOT EXISTS crawler_cache (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    blueprint_code VARCHAR(128) NOT NULL,
    cache_key VARCHAR(256) NOT NULL,
    response_blob LONGBLOB,
    expires_at TIMESTAMP NULL,
    UNIQUE KEY uk_crawler_cache (blueprint_code, cache_key)
);

-- Create flyway_schema_history table to prevent flyway from running
CREATE TABLE IF NOT EXISTS flyway_schema_history (
    installed_rank INT NOT NULL,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INT,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_time INT NOT NULL,
    success BOOLEAN NOT NULL,
    PRIMARY KEY (installed_rank),
    INDEX flyway_schema_history_s_idx (success)
);

-- Insert fake flyway records so it thinks migrations are done
INSERT IGNORE INTO flyway_schema_history 
(installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES 
(1, '1', '<< Flyway Baseline >>', 'BASELINE', '<< Flyway Baseline >>', NULL, 'manual', NOW(), 0, 1),
(2, '10', 'Manual DB Setup', 'SQL', 'manual-db-setup.sql', 999999999, 'manual', NOW(), 1000, 1);

SELECT 'Database setup completed successfully!' as status;
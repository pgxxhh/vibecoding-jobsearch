-- Create missing tables for production environment
-- This migration ensures all required tables exist regardless of previous migration state

-- Job data source tables
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

-- Crawler tables
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
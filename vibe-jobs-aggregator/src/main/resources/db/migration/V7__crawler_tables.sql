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

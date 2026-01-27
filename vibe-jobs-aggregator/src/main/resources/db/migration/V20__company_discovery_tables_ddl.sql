-- Company discovery settings
CREATE TABLE IF NOT EXISTS company_discovery_settings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    setting_key VARCHAR(128) NOT NULL,
    setting_value LONGTEXT NOT NULL,
    description VARCHAR(255),
    updated_by VARCHAR(255),
    create_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE KEY uk_company_discovery_settings_key (setting_key),
    INDEX idx_company_discovery_settings_deleted (deleted)
);

-- Company discovery run log
CREATE TABLE IF NOT EXISTS company_discovery_run (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    status VARCHAR(32) NOT NULL,
    provider VARCHAR(128),
    dry_run BOOLEAN NOT NULL DEFAULT TRUE,
    total_candidates INT NOT NULL DEFAULT 0,
    total_valid INT NOT NULL DEFAULT 0,
    started_at TIMESTAMP(3) NOT NULL,
    completed_at TIMESTAMP(3),
    create_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_company_discovery_run_started (started_at),
    INDEX idx_company_discovery_run_status (status),
    INDEX idx_company_discovery_run_deleted (deleted)
);

-- Company discovery results
CREATE TABLE IF NOT EXISTS company_discovery_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    run_id BIGINT NOT NULL,
    data_source_code VARCHAR(128) NOT NULL,
    company_reference VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    provider VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    reason VARCHAR(512),
    create_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_company_discovery_result_run (run_id),
    INDEX idx_company_discovery_result_code (data_source_code),
    INDEX idx_company_discovery_result_status (status),
    INDEX idx_company_discovery_result_deleted (deleted)
);

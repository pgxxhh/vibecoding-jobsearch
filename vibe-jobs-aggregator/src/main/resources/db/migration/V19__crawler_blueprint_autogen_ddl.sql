CREATE TABLE IF NOT EXISTS crawler_blueprint_generation_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    blueprint_code VARCHAR(128) NOT NULL,
    input_payload JSON,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    started_at TIMESTAMP NULL,
    finished_at TIMESTAMP NULL,
    error_message TEXT,
    browser_session_snapshot JSON,
    sample_data JSON,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    INDEX idx_generation_task_blueprint (blueprint_code),
    INDEX idx_generation_task_status (status)
);

ALTER TABLE crawler_blueprint
    ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS draft_config_json JSON NULL,
    ADD COLUMN IF NOT EXISTS last_test_report JSON NULL,
    ADD COLUMN IF NOT EXISTS auto_generated TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS generated_by VARCHAR(128) NULL,
    ADD COLUMN IF NOT EXISTS generated_at TIMESTAMP NULL;

ALTER TABLE job_data_source
    ADD COLUMN IF NOT EXISTS crawler_blueprint_code VARCHAR(128) NULL,
    ADD COLUMN IF NOT EXISTS auto_generated TINYINT(1) NOT NULL DEFAULT 0;

-- DDL for job alert subscription feature (mirrors Flyway migration V12)
CREATE TABLE IF NOT EXISTS job_alert_subscription (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BINARY(16) NOT NULL,
    email VARCHAR(255) NOT NULL,
    search_keyword VARCHAR(255) NULL,
    company VARCHAR(255) NULL,
    location VARCHAR(255) NULL,
    level VARCHAR(100) NULL,
    filters_json LONGTEXT NULL,
    schedule_hour TINYINT NOT NULL DEFAULT 0,
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai',
    last_notified_at DATETIME NULL,
    last_seen_cursor VARBINARY(64) NULL,
    status ENUM('ACTIVE','PAUSED','CANCELLED') NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_job_alert_user FOREIGN KEY (user_id) REFERENCES auth_user(id)
);

CREATE INDEX IF NOT EXISTS idx_job_alert_user_status ON job_alert_subscription (user_id, status);
CREATE INDEX IF NOT EXISTS idx_job_alert_schedule ON job_alert_subscription (status, schedule_hour);

CREATE TABLE IF NOT EXISTS job_alert_delivery (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    subscription_id BIGINT NOT NULL,
    delivered_at DATETIME NOT NULL,
    job_count INT NOT NULL DEFAULT 0,
    job_ids LONGTEXT NULL,
    status ENUM('SENT','SKIPPED','FAILED') NOT NULL,
    error_message TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_job_alert_delivery_subscription FOREIGN KEY (subscription_id) REFERENCES job_alert_subscription(id)
);

CREATE INDEX IF NOT EXISTS idx_job_alert_delivery_subscription ON job_alert_delivery (subscription_id, delivered_at DESC);

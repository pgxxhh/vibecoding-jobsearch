-- Manual bootstrap for admin tables previously managed by Flyway V8
-- Run alongside simple-db-setup.sql when seeding a new environment without Flyway

CREATE TABLE IF NOT EXISTS ingestion_settings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    setting_key VARCHAR(128) NOT NULL,
    setting_value LONGTEXT,
    description VARCHAR(255),
    updated_by VARCHAR(320),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ingestion_settings_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_change_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_email VARCHAR(320) NOT NULL,
    change_type VARCHAR(128) NOT NULL,
    entity_type VARCHAR(128) NOT NULL,
    entity_id VARCHAR(128),
    details LONGTEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_admin_change_log_created_at (created_at),
    KEY idx_admin_change_log_actor (actor_email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_allowed_email (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(320) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_allowed_email_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO admin_allowed_email (email) VALUES
    ('975022570yp@gmail.com');

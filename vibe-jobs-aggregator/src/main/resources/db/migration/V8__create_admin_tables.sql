CREATE TABLE ingestion_settings (
    id BIGINT PRIMARY KEY,
    settings_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE admin_change_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_email VARCHAR(255) NOT NULL,
    action VARCHAR(64) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(128),
    diff_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE admin_allowed_email (
    email VARCHAR(255) PRIMARY KEY
);

INSERT INTO admin_allowed_email(email) VALUES ('975022570yp@gmail.com');

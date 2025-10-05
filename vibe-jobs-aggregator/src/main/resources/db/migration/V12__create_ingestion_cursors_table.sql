CREATE TABLE IF NOT EXISTS ingestion_cursors (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    source_code VARCHAR(128) NOT NULL,
    source_name VARCHAR(128) NOT NULL,
    company VARCHAR(128) NOT NULL,
    category VARCHAR(128) NOT NULL,
    last_posted_at TIMESTAMP NULL,
    last_external_id VARCHAR(256) NULL,
    next_page_token VARCHAR(512) NULL,
    last_ingested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE KEY uq_ingestion_cursor (source_name, company, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

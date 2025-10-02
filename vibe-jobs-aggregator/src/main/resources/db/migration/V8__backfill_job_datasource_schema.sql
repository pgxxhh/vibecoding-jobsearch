-- Backfill migration ensuring datasource schema exists for environments stalled before V4
-- Recreates tables and schema changes from V4-V7 using idempotent statements so it can be
-- safely applied on systems that only ran migrations up to version 3.

-- Ensure job data source base tables exist (from V4)
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
    data_source_id BIGINT NOT NULL,
    reference VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    slug VARCHAR(255),
    enabled BIT NOT NULL DEFAULT 1,
    placeholder_overrides TEXT,
    override_options TEXT,
    CONSTRAINT fk_job_data_source_company_source FOREIGN KEY (data_source_id) REFERENCES job_data_source (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_job_data_source_company_source ON job_data_source_company (data_source_id);

CREATE TABLE IF NOT EXISTS job_data_source_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    data_source_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    quota_limit INT NOT NULL,
    tags TEXT,
    facets TEXT,
    CONSTRAINT fk_job_data_source_category_source FOREIGN KEY (data_source_id) REFERENCES job_data_source (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_job_data_source_category_source ON job_data_source_category (data_source_id);

-- Add code columns to child tables (from V5)
ALTER TABLE job_data_source_company ADD COLUMN IF NOT EXISTS data_source_code VARCHAR(128);
ALTER TABLE job_data_source_category ADD COLUMN IF NOT EXISTS data_source_code VARCHAR(128);

-- Backfill code values when legacy id column is still present
SET @company_has_id := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'job_data_source_company'
      AND COLUMN_NAME = 'data_source_id'
);
SET @sql := IF(@company_has_id > 0,
    'UPDATE job_data_source_company jdsc INNER JOIN job_data_source jds ON jdsc.data_source_id = jds.id SET jdsc.data_source_code = jds.code WHERE jdsc.data_source_code IS NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @category_has_id := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'job_data_source_category'
      AND COLUMN_NAME = 'data_source_id'
);
SET @sql := IF(@category_has_id > 0,
    'UPDATE job_data_source_category jdsc INNER JOIN job_data_source jds ON jdsc.data_source_id = jds.id SET jdsc.data_source_code = jds.code WHERE jdsc.data_source_code IS NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure code columns are non-nullable
ALTER TABLE job_data_source_company MODIFY COLUMN data_source_code VARCHAR(128) NOT NULL;
ALTER TABLE job_data_source_category MODIFY COLUMN data_source_code VARCHAR(128) NOT NULL;

-- Drop legacy foreign keys when present
SET @fk_company_source := (
    SELECT CONSTRAINT_NAME
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'job_data_source_company'
      AND CONSTRAINT_NAME = 'fk_job_data_source_company_source'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @sql := IF(@fk_company_source IS NOT NULL,
    CONCAT('ALTER TABLE job_data_source_company DROP FOREIGN KEY ', @fk_company_source),
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_category_source := (
    SELECT CONSTRAINT_NAME
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'job_data_source_category'
      AND CONSTRAINT_NAME = 'fk_job_data_source_category_source'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @sql := IF(@fk_category_source IS NOT NULL,
    CONCAT('ALTER TABLE job_data_source_category DROP FOREIGN KEY ', @fk_category_source),
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure new foreign keys exist on code columns
SET @fk_company_code := (
    SELECT CONSTRAINT_NAME
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'job_data_source_company'
      AND CONSTRAINT_NAME = 'fk_job_data_source_company_code'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @sql := IF(@fk_company_code IS NULL,
    'ALTER TABLE job_data_source_company ADD CONSTRAINT fk_job_data_source_company_code FOREIGN KEY (data_source_code) REFERENCES job_data_source (code) ON DELETE CASCADE',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_category_code := (
    SELECT CONSTRAINT_NAME
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'job_data_source_category'
      AND CONSTRAINT_NAME = 'fk_job_data_source_category_code'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @sql := IF(@fk_category_code IS NULL,
    'ALTER TABLE job_data_source_category ADD CONSTRAINT fk_job_data_source_category_code FOREIGN KEY (data_source_code) REFERENCES job_data_source (code) ON DELETE CASCADE',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop legacy indexes and add new ones guarded for idempotency
SET @old_idx_company := (
    SELECT INDEX_NAME
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'job_data_source_company'
      AND INDEX_NAME = 'idx_job_data_source_company_source'
);
SET @sql := IF(@old_idx_company IS NOT NULL,
    'DROP INDEX idx_job_data_source_company_source ON job_data_source_company',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @old_idx_category := (
    SELECT INDEX_NAME
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'job_data_source_category'
      AND INDEX_NAME = 'idx_job_data_source_category_source'
);
SET @sql := IF(@old_idx_category IS NOT NULL,
    'DROP INDEX idx_job_data_source_category_source ON job_data_source_category',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE INDEX IF NOT EXISTS idx_job_data_source_company_code ON job_data_source_company (data_source_code);
CREATE INDEX IF NOT EXISTS idx_job_data_source_category_code ON job_data_source_category (data_source_code);

-- Drop legacy id columns and clean orphaned rows (from V6)
ALTER TABLE job_data_source_company DROP COLUMN IF EXISTS data_source_id;
ALTER TABLE job_data_source_category DROP COLUMN IF EXISTS data_source_id;

DELETE jdsc FROM job_data_source_company jdsc
LEFT JOIN job_data_source jds ON jdsc.data_source_code = jds.code
WHERE jds.code IS NULL;

DELETE jdsc FROM job_data_source_category jdsc
LEFT JOIN job_data_source jds ON jdsc.data_source_code = jds.code
WHERE jds.code IS NULL;

-- Ensure crawler tables exist (from V7)
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

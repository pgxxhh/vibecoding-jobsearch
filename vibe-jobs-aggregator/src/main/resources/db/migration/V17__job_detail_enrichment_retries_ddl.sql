-- Add retry tracking fields to job_detail_enrichments using idempotent checks so the script
-- can be executed multiple times on MySQL 5.7+/8.x and H2.

SET @table_name := 'job_detail_enrichments';

-- status_state column
SET @has_status_state := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @table_name
      AND COLUMN_NAME = 'status_state'
);
SET @sql := IF(@has_status_state = 0,
    'ALTER TABLE job_detail_enrichments ADD COLUMN status_state VARCHAR(32) NULL AFTER metadata_json',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- retry_count column
SET @has_retry_count := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @table_name
      AND COLUMN_NAME = 'retry_count'
);
SET @sql := IF(@has_retry_count = 0,
    'ALTER TABLE job_detail_enrichments ADD COLUMN retry_count INT NOT NULL DEFAULT 0 AFTER status_state',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- next_retry_at column
SET @has_next_retry_at := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @table_name
      AND COLUMN_NAME = 'next_retry_at'
);
SET @sql := IF(@has_next_retry_at = 0,
    'ALTER TABLE job_detail_enrichments ADD COLUMN next_retry_at TIMESTAMP NULL AFTER retry_count',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- last_attempt_at column
SET @has_last_attempt_at := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @table_name
      AND COLUMN_NAME = 'last_attempt_at'
);
SET @sql := IF(@has_last_attempt_at = 0,
    'ALTER TABLE job_detail_enrichments ADD COLUMN last_attempt_at TIMESTAMP NULL AFTER next_retry_at',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- max_attempts column
SET @has_max_attempts := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @table_name
      AND COLUMN_NAME = 'max_attempts'
);
SET @sql := IF(@has_max_attempts = 0,
    'ALTER TABLE job_detail_enrichments ADD COLUMN max_attempts INT NULL AFTER last_attempt_at',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- index on (enrichment_key, status_state, next_retry_at)
SET @has_index := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @table_name
      AND INDEX_NAME = 'idx_job_detail_enrichments_status_retry'
);
SET @sql := IF(@has_index = 0,
    'CREATE INDEX idx_job_detail_enrichments_status_retry ON job_detail_enrichments (enrichment_key, status_state, next_retry_at)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'jobs'
      AND INDEX_NAME = 'idx_jobs_title_company_location_ft'
);

SET @ddl := IF(
    @index_exists = 0,
    'CREATE FULLTEXT INDEX idx_jobs_title_company_location_ft ON jobs (title, company, location)',
    'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

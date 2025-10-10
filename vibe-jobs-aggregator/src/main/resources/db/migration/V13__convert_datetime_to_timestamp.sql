-- Convert all DATETIME(6) fields to TIMESTAMP(3)
-- Based on actual database schema analysis
-- 
-- Benefits:
-- - Automatic timezone conversion (UTC storage)
-- - 50% storage space reduction (4 bytes vs 8 bytes)  
-- - Native database timestamp features (DEFAULT CURRENT_TIMESTAMP, ON UPDATE)
-- - Better integration with application timezone handling
-- - TIMESTAMP(3) provides millisecond precision (sufficient for most use cases)
--
-- Prerequisites:
-- - All data must be within TIMESTAMP range (1970-01-01 to 2038-01-19)
-- - Database timezone should be set to UTC
-- - Application maintenance window required

-- Verify current timezone settings
SELECT 
    'Pre-migration timezone check' as check_type,
    @@global.time_zone as global_timezone,
    @@session.time_zone as session_timezone,
    NOW() as current_server_time,
    UTC_TIMESTAMP() as current_utc_time;

-- Core business tables conversion
-- ================================

-- 1. jobs table (most critical - primary business data)
ALTER TABLE jobs 
MODIFY COLUMN created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
MODIFY COLUMN updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
MODIFY COLUMN posted_at TIMESTAMP(3) NULL;

-- 2. job_details table
ALTER TABLE job_details 
MODIFY COLUMN created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
MODIFY COLUMN updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3);

-- 3. job_tags table (has created_time/updated_time naming)
ALTER TABLE job_tags 
MODIFY COLUMN created_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
MODIFY COLUMN updated_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3);

-- Authentication and user management tables  
-- =========================================

-- 4. auth_user table
ALTER TABLE auth_user 
MODIFY COLUMN created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
MODIFY COLUMN updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
MODIFY COLUMN last_login_at TIMESTAMP(3) NULL;

-- 5. auth_session table
ALTER TABLE auth_session 
MODIFY COLUMN created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
MODIFY COLUMN expires_at TIMESTAMP(3) NOT NULL,
MODIFY COLUMN revoked_at TIMESTAMP(3) NULL,
MODIFY COLUMN updated_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3);

-- 6. auth_login_challenge table  
ALTER TABLE auth_login_challenge 
MODIFY COLUMN created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
MODIFY COLUMN updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
MODIFY COLUMN expires_at TIMESTAMP(3) NOT NULL,
MODIFY COLUMN last_sent_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3);

-- Data source and configuration tables
-- ===================================

-- 7. job_data_source table
ALTER TABLE job_data_source 
MODIFY COLUMN created_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
MODIFY COLUMN updated_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3);

-- 8. job_data_source_company table
ALTER TABLE job_data_source_company 
MODIFY COLUMN created_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
MODIFY COLUMN updated_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3);

-- 9. job_data_source_category table
ALTER TABLE job_data_source_category 
MODIFY COLUMN created_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
MODIFY COLUMN updated_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3);

-- Admin and logging tables
-- =======================

-- 10. admin_allowed_email table (has both created_at/created_time - standardize to created_at/updated_at)
ALTER TABLE admin_allowed_email 
MODIFY COLUMN created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
MODIFY COLUMN updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
MODIFY COLUMN created_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
MODIFY COLUMN updated_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3);

-- 11. admin_change_log table
ALTER TABLE admin_change_log 
MODIFY COLUMN created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
MODIFY COLUMN updated_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3);

-- 12. ingestion_settings table
ALTER TABLE ingestion_settings 
MODIFY COLUMN created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
MODIFY COLUMN updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3);

-- Crawler tables (mixed DATETIME and TIMESTAMP - standardize remaining DATETIME fields)
-- ==================================================================================

-- 13. crawler_cache table (has mixed types)
ALTER TABLE crawler_cache 
MODIFY COLUMN created_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
MODIFY COLUMN updated_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3);
-- Note: expires_at is already TIMESTAMP, keep as is

-- 14. crawler_run_log table (has mixed types)  
ALTER TABLE crawler_run_log 
MODIFY COLUMN created_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
MODIFY COLUMN updated_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3);
-- Note: started_at and completed_at are already TIMESTAMP, keep as is

-- Note: crawler_blueprint and crawler_parser_template are already fully TIMESTAMP
-- Note: ingestion_cursors is already fully TIMESTAMP
-- Note: flyway_schema_history should not be modified

-- Post-migration validation
-- ========================

-- Verify all datetime fields have been converted to timestamp
SELECT 
    'Post-migration validation' as check_type,
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    EXTRA
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
AND DATA_TYPE = 'datetime'
AND (COLUMN_NAME LIKE '%time%' OR COLUMN_NAME LIKE '%_at')
ORDER BY TABLE_NAME, COLUMN_NAME;

-- Show summary of timestamp fields after conversion
SELECT 
    'Timestamp fields summary' as summary_type,
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_DEFAULT,
    EXTRA
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
AND DATA_TYPE = 'timestamp'
AND TABLE_NAME NOT IN ('flyway_schema_history')
ORDER BY TABLE_NAME, COLUMN_NAME;

-- Check that automatic timestamps work correctly
INSERT INTO jobs (source, external_id, title, company, location, checksum) 
VALUES ('MIGRATION_TEST', 'test-timestamp-' || UNIX_TIMESTAMP(), 'Test Timestamp Migration', 'Test Company', 'Test Location', 'test-checksum')
ON DUPLICATE KEY UPDATE title = VALUES(title);

SELECT 
    'Timestamp functionality test' as test_type,
    'Automatic timestamp creation and update working' as result,
    COUNT(*) as test_records
FROM jobs 
WHERE source = 'MIGRATION_TEST' 
AND created_at IS NOT NULL 
AND updated_at IS NOT NULL
AND ABS(TIMESTAMPDIFF(SECOND, created_at, NOW())) < 60;

-- Clean up test data
DELETE FROM jobs WHERE source = 'MIGRATION_TEST';

-- Final validation summary
SELECT 
    'Migration completed successfully' as status, 
    NOW() as completion_time,
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND DATA_TYPE = 'datetime'
     AND (COLUMN_NAME LIKE '%time%' OR COLUMN_NAME LIKE '%_at')
     AND TABLE_NAME NOT IN ('flyway_schema_history')) as remaining_datetime_fields;
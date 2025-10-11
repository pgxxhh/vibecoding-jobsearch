SET @schema_name = DATABASE();

SELECT COUNT(*) INTO @has_summary
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = @schema_name
  AND TABLE_NAME = 'job_details'
  AND COLUMN_NAME = 'summary';

SET @drop_summary_sql = IF(@has_summary > 0,
    'ALTER TABLE job_details DROP COLUMN summary',
    'SELECT 1');

PREPARE drop_summary_stmt FROM @drop_summary_sql;
EXECUTE drop_summary_stmt;
DEALLOCATE PREPARE drop_summary_stmt;

SELECT COUNT(*) INTO @has_structured_data
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = @schema_name
  AND TABLE_NAME = 'job_details'
  AND COLUMN_NAME = 'structured_data';

SET @drop_structured_sql = IF(@has_structured_data > 0,
    'ALTER TABLE job_details DROP COLUMN structured_data',
    'SELECT 1');

PREPARE drop_structured_stmt FROM @drop_structured_sql;
EXECUTE drop_structured_stmt;
DEALLOCATE PREPARE drop_structured_stmt;

DROP TABLE IF EXISTS job_detail_skills;
DROP TABLE IF EXISTS job_detail_highlights;

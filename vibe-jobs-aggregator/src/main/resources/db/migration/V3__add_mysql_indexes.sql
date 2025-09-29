-- Add composite and supporting indexes for common job queries. Each statement
-- only runs when the target index is missing so Flyway stays idempotent even if
-- the indexes were created manually beforehand.

SET @schema_name := (SELECT DATABASE());

-- jobs(posted_at DESC, id DESC)
SET @sql := (
    SELECT IF (
        EXISTS (
            SELECT 1 FROM information_schema.statistics
            WHERE table_schema = @schema_name
              AND table_name = 'jobs'
              AND index_name = 'idx_jobs_posted_at_id_desc'
        ),
        'SELECT 1',
        'CREATE INDEX idx_jobs_posted_at_id_desc ON jobs (posted_at DESC, id DESC)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- jobs(LOWER(company), posted_at)
SET @sql := (
    SELECT IF (
        EXISTS (
            SELECT 1 FROM information_schema.statistics
            WHERE table_schema = @schema_name
              AND table_name = 'jobs'
              AND index_name = 'idx_jobs_company_posted_at'
        ),
        'SELECT 1',
        'CREATE INDEX idx_jobs_company_posted_at ON jobs ((LOWER(company)), posted_at)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- jobs(LOWER(location), posted_at)
SET @sql := (
    SELECT IF (
        EXISTS (
            SELECT 1 FROM information_schema.statistics
            WHERE table_schema = @schema_name
              AND table_name = 'jobs'
              AND index_name = 'idx_jobs_location_posted_at'
        ),
        'SELECT 1',
        'CREATE INDEX idx_jobs_location_posted_at ON jobs ((LOWER(location)), posted_at)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- jobs(level)
SET @sql := (
    SELECT IF (
        EXISTS (
            SELECT 1 FROM information_schema.statistics
            WHERE table_schema = @schema_name
              AND table_name = 'jobs'
              AND index_name = 'idx_jobs_level'
        ),
        'SELECT 1',
        'CREATE INDEX idx_jobs_level ON jobs (level)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- job_tags(tag)
SET @sql := (
    SELECT IF (
        EXISTS (
            SELECT 1 FROM information_schema.statistics
            WHERE table_schema = @schema_name
              AND table_name = 'job_tags'
              AND index_name = 'idx_job_tags_tag'
        ),
        'SELECT 1',
        'CREATE INDEX idx_job_tags_tag ON job_tags (tag)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- job_tags(job_id, tag)
SET @sql := (
    SELECT IF (
        EXISTS (
            SELECT 1 FROM information_schema.statistics
            WHERE table_schema = @schema_name
              AND table_name = 'job_tags'
              AND index_name = 'idx_job_tags_job_tag'
        ),
        'SELECT 1',
        'CREATE INDEX idx_job_tags_job_tag ON job_tags (job_id, tag)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

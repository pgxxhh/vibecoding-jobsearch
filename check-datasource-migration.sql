-- Check if job data sources exist in the database
-- This will help us understand why the configuration migration didn't run

-- Check if job_data_source table exists and has any records
SELECT 
    'job_data_source table' as check_type,
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM information_schema.tables 
            WHERE table_schema = DATABASE() AND table_name = 'job_data_source'
        ) THEN 'EXISTS' 
        ELSE 'MISSING' 
    END as table_status,
    COALESCE((SELECT COUNT(*) FROM job_data_source), 0) as record_count;

-- If table exists, show current records
SELECT 'Current job_data_source records:' as info;
SELECT code, type, enabled, run_on_startup FROM job_data_source;

-- Check company records
SELECT 
    'job_data_source_company table' as check_type,
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM information_schema.tables 
            WHERE table_schema = DATABASE() AND table_name = 'job_data_source_company'
        ) THEN 'EXISTS' 
        ELSE 'MISSING' 
    END as table_status,
    COALESCE((SELECT COUNT(*) FROM job_data_source_company), 0) as record_count;

SELECT 'Current job_data_source_company records:' as info;
SELECT data_source_code, reference, display_name, enabled FROM job_data_source_company LIMIT 10;
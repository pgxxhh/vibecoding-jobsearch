-- Add soft delete and timestamp fields to job_data_source_company table

-- Add deleted field if it doesn't exist (NOT NULL with default FALSE)
ALTER TABLE job_data_source_company 
ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Add created_time field if it doesn't exist
ALTER TABLE job_data_source_company 
ADD COLUMN IF NOT EXISTS created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add updated_time field if it doesn't exist  
ALTER TABLE job_data_source_company 
ADD COLUMN IF NOT EXISTS updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Ensure all existing records have deleted = false (update any potential NULL values)
UPDATE job_data_source_company SET deleted = FALSE WHERE deleted IS NULL;

-- Make sure the deleted column is NOT NULL if it was previously nullable
ALTER TABLE job_data_source_company MODIFY COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Ensure all existing records have proper timestamps (in case columns existed but had NULL values)
UPDATE job_data_source_company 
SET created_time = COALESCE(created_time, CURRENT_TIMESTAMP),
    updated_time = COALESCE(updated_time, CURRENT_TIMESTAMP)
WHERE created_time IS NULL OR updated_time IS NULL;

-- Add index for deleted field for better query performance
CREATE INDEX IF NOT EXISTS idx_job_data_source_company_deleted ON job_data_source_company (deleted);

-- Add composite index for data_source_code and deleted for optimal filtering
CREATE INDEX IF NOT EXISTS idx_job_data_source_company_code_deleted ON job_data_source_company (data_source_code, deleted);
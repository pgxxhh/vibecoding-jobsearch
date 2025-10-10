ALTER TABLE job_details
    ADD COLUMN IF NOT EXISTS content_text LONGTEXT;

/*!50700 CREATE FULLTEXT INDEX IF NOT EXISTS idx_job_details_content_text ON job_details (content_text); */

ALTER TABLE job_details
    ADD COLUMN content_text LONGTEXT,
    ADD FULLTEXT INDEX idx_job_details_content_text (content_text);

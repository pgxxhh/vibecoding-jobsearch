-- Add composite and supporting indexes for common job queries
CREATE INDEX idx_jobs_posted_at_id_desc ON jobs (posted_at DESC, id DESC);
CREATE INDEX idx_jobs_company_posted_at ON jobs ((LOWER(company)), posted_at);
CREATE INDEX idx_jobs_location_posted_at ON jobs ((LOWER(location)), posted_at);
CREATE INDEX idx_jobs_level ON jobs (level);

-- Improve tag lookups and joins
CREATE INDEX idx_job_tags_tag ON job_tags (tag);
CREATE INDEX idx_job_tags_job_tag ON job_tags (job_id, tag);

-- Add composite and supporting indexes for common job queries
-- Using simple CREATE INDEX IF NOT EXISTS for better compatibility

-- jobs(posted_at DESC, id DESC)
CREATE INDEX IF NOT EXISTS idx_jobs_posted_at_id_desc ON jobs (posted_at DESC, id DESC);

-- jobs(LOWER(company), posted_at) - using functional index
CREATE INDEX IF NOT EXISTS idx_jobs_company_posted_at ON jobs ((LOWER(company)), posted_at);

-- jobs(LOWER(location), posted_at) - using functional index
CREATE INDEX IF NOT EXISTS idx_jobs_location_posted_at ON jobs ((LOWER(location)), posted_at);

-- jobs(level)
CREATE INDEX IF NOT EXISTS idx_jobs_level ON jobs (level);

-- job_tags(tag)
CREATE INDEX IF NOT EXISTS idx_job_tags_tag ON job_tags (tag);

-- job_tags(job_id, tag)
CREATE INDEX IF NOT EXISTS idx_job_tags_job_tag ON job_tags (job_id, tag);

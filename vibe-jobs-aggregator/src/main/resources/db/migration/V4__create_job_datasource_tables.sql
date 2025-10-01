CREATE TABLE IF NOT EXISTS job_data_source (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(128) NOT NULL UNIQUE,
    type VARCHAR(128) NOT NULL,
    enabled BIT NOT NULL DEFAULT 1,
    run_on_startup BIT NOT NULL DEFAULT 1,
    require_override BIT NOT NULL DEFAULT 0,
    flow VARCHAR(16) NOT NULL DEFAULT 'UNLIMITED',
    base_options TEXT
);

CREATE TABLE IF NOT EXISTS job_data_source_company (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    data_source_id BIGINT NOT NULL,
    reference VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    slug VARCHAR(255),
    enabled BIT NOT NULL DEFAULT 1,
    placeholder_overrides TEXT,
    override_options TEXT,
    CONSTRAINT fk_job_data_source_company_source FOREIGN KEY (data_source_id) REFERENCES job_data_source (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_job_data_source_company_source ON job_data_source_company (data_source_id);

CREATE TABLE IF NOT EXISTS job_data_source_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    data_source_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    quota_limit INT NOT NULL,
    tags TEXT,
    facets TEXT,
    CONSTRAINT fk_job_data_source_category_source FOREIGN KEY (data_source_id) REFERENCES job_data_source (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_job_data_source_category_source ON job_data_source_category (data_source_id);

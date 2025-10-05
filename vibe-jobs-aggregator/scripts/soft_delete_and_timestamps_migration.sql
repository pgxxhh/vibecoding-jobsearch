-- 软删除和时间戳字段迁移脚本
-- 1. 添加 deleted 字段（TINYINT(1) DEFAULT 0，表示未删除）
-- 2. 添加缺失的 created_time 和 updated_time 字段
-- 运行此脚本前请先备份数据库

-- ================================
-- 主要业务表：jobs 相关
-- ================================

-- jobs 表已有 created_at, updated_at，只需添加 deleted 字段
ALTER TABLE jobs ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
CREATE INDEX idx_jobs_deleted ON jobs (deleted);

-- job_details 表已有 created_at, updated_at，只需添加 deleted 字段  
ALTER TABLE job_details ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
CREATE INDEX idx_job_details_deleted ON job_details (deleted);

-- job_tags 表没有时间字段，需要添加完整的字段
ALTER TABLE job_tags 
ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0,
ADD COLUMN created_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
ADD COLUMN updated_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);
CREATE INDEX idx_job_tags_deleted ON job_tags (deleted);
CREATE INDEX idx_job_tags_created_time ON job_tags (created_time);

-- ================================
-- 数据源相关表
-- ================================

-- job_data_source 表没有时间字段，需要添加完整字段
ALTER TABLE job_data_source 
ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0,
ADD COLUMN created_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
ADD COLUMN updated_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);
CREATE INDEX idx_job_data_source_deleted ON job_data_source (deleted);
CREATE INDEX idx_job_data_source_created_time ON job_data_source (created_time);

-- job_data_source_company 表没有时间字段，需要添加完整字段
ALTER TABLE job_data_source_company 
ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0,
ADD COLUMN created_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
ADD COLUMN updated_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);
CREATE INDEX idx_job_data_source_company_deleted ON job_data_source_company (deleted);
CREATE INDEX idx_job_data_source_company_created_time ON job_data_source_company (created_time);

-- job_data_source_category 表没有时间字段，需要添加完整字段
ALTER TABLE job_data_source_category 
ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0,
ADD COLUMN created_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
ADD COLUMN updated_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);
CREATE INDEX idx_job_data_source_category_deleted ON job_data_source_category (deleted);
CREATE INDEX idx_job_data_source_category_created_time ON job_data_source_category (created_time);

-- ================================
-- 爬虫相关表
-- ================================

-- crawler_blueprint 表已有 created_at, updated_at，只需添加 deleted 字段
ALTER TABLE crawler_blueprint ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
CREATE INDEX idx_crawler_blueprint_deleted ON crawler_blueprint (deleted);

-- crawler_parser_template 表已有 created_at, updated_at，只需添加 deleted 字段
ALTER TABLE crawler_parser_template ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
CREATE INDEX idx_crawler_parser_template_deleted ON crawler_parser_template (deleted);

-- crawler_run_log 表有 started_at, completed_at，需要添加标准时间字段和 deleted 字段
ALTER TABLE crawler_run_log 
ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0,
ADD COLUMN created_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
ADD COLUMN updated_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);
CREATE INDEX idx_crawler_run_log_deleted ON crawler_run_log (deleted);
CREATE INDEX idx_crawler_run_log_created_time ON crawler_run_log (created_time);

-- crawler_cache 表有 expires_at，需要添加标准时间字段和 deleted 字段
ALTER TABLE crawler_cache 
ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0,
ADD COLUMN created_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
ADD COLUMN updated_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);
CREATE INDEX idx_crawler_cache_deleted ON crawler_cache (deleted);
CREATE INDEX idx_crawler_cache_created_time ON crawler_cache (created_time);

-- ================================
-- 认证相关表
-- ================================

-- auth_user 表已有 created_at, updated_at，只需添加 deleted 字段
ALTER TABLE auth_user ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
CREATE INDEX idx_auth_user_deleted ON auth_user (deleted);

-- auth_login_challenge 表已有 created_at, updated_at，只需添加 deleted 字段
ALTER TABLE auth_login_challenge ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
CREATE INDEX idx_auth_login_challenge_deleted ON auth_login_challenge (deleted);

-- auth_session 表已有 created_at，没有 updated_at，需要添加 updated_time 和 deleted 字段
ALTER TABLE auth_session 
ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0,
ADD COLUMN updated_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);
CREATE INDEX idx_auth_session_deleted ON auth_session (deleted);

-- ================================
-- 管理相关表
-- ================================

-- admin_change_log 表已有 created_at，需要添加 updated_time 和 deleted 字段
ALTER TABLE admin_change_log 
ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0,
ADD COLUMN updated_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);
CREATE INDEX idx_admin_change_log_deleted ON admin_change_log (deleted);

-- admin_allowed_email 表没有时间字段，需要添加完整字段
ALTER TABLE admin_allowed_email 
ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0,
ADD COLUMN created_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
ADD COLUMN updated_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);
CREATE INDEX idx_admin_allowed_email_deleted ON admin_allowed_email (deleted);
CREATE INDEX idx_admin_allowed_email_created_time ON admin_allowed_email (created_time);

-- ingestion_settings 表已有 created_at, updated_at，只需添加 deleted 字段
ALTER TABLE ingestion_settings ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
CREATE INDEX idx_ingestion_settings_deleted ON ingestion_settings (deleted);

-- ================================
-- 完成提示
-- ================================

SELECT 
    'Soft delete migration completed successfully!' as status,
    'All tables now have deleted field (TINYINT(1) DEFAULT 0)' as deleted_field,
    'Missing created_time and updated_time fields have been added' as timestamp_fields,
    'Remember to update application code to filter by deleted = 0' as reminder;
-- 重构数据源关联关系，使用 code 而不是 id 进行关联
-- 这样设计更符合业务逻辑，配置更加直观

-- 1. 添加新的 code 字段到子表
ALTER TABLE job_data_source_company ADD COLUMN data_source_code VARCHAR(128);
ALTER TABLE job_data_source_category ADD COLUMN data_source_code VARCHAR(128);

-- 2. 将现有数据的 id 关联转换为 code 关联
UPDATE job_data_source_company jdsc 
INNER JOIN job_data_source jds ON jdsc.data_source_id = jds.id 
SET jdsc.data_source_code = jds.code;

UPDATE job_data_source_category jdsc 
INNER JOIN job_data_source jds ON jdsc.data_source_id = jds.id 
SET jdsc.data_source_code = jds.code;

-- 3. 设置新字段为非空约束
ALTER TABLE job_data_source_company MODIFY COLUMN data_source_code VARCHAR(128) NOT NULL;
ALTER TABLE job_data_source_category MODIFY COLUMN data_source_code VARCHAR(128) NOT NULL;

-- 4. 删除旧的外键约束
ALTER TABLE job_data_source_company DROP FOREIGN KEY fk_job_data_source_company_source;
ALTER TABLE job_data_source_category DROP FOREIGN KEY fk_job_data_source_category_source;

-- 5. 添加新的外键约束，基于 code 字段
ALTER TABLE job_data_source_company 
ADD CONSTRAINT fk_job_data_source_company_code 
FOREIGN KEY (data_source_code) REFERENCES job_data_source (code) ON DELETE CASCADE;

ALTER TABLE job_data_source_category 
ADD CONSTRAINT fk_job_data_source_category_code 
FOREIGN KEY (data_source_code) REFERENCES job_data_source (code) ON DELETE CASCADE;

-- 6. 删除旧的索引并创建新索引
DROP INDEX idx_job_data_source_company_source ON job_data_source_company;
DROP INDEX idx_job_data_source_category_source ON job_data_source_category;

CREATE INDEX idx_job_data_source_company_code ON job_data_source_company (data_source_code);
CREATE INDEX idx_job_data_source_category_code ON job_data_source_category (data_source_code);

-- 7. 最后删除旧的 id 关联字段（在确认数据正确后）
-- ALTER TABLE job_data_source_company DROP COLUMN data_source_id;
-- ALTER TABLE job_data_source_category DROP COLUMN data_source_id;

-- 注意：暂时保留 data_source_id 字段，以便在验证数据正确性后再删除
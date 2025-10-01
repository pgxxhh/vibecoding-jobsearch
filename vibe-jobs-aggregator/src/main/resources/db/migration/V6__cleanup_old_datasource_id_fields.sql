-- 清理旧的 data_source_id 字段，完成向基于 code 关联的迁移
-- 这是 V5 迁移的后续清理步骤

-- 1. 删除旧的 data_source_id 字段
ALTER TABLE job_data_source_company DROP COLUMN data_source_id;
ALTER TABLE job_data_source_category DROP COLUMN data_source_id;

-- 2. 验证数据完整性 - 确保所有记录都有有效的 data_source_code
-- 如果有孤儿记录（data_source_code 不存在对应的 job_data_source），则删除
DELETE jdsc FROM job_data_source_company jdsc
LEFT JOIN job_data_source jds ON jdsc.data_source_code = jds.code
WHERE jds.code IS NULL;

DELETE jdsc FROM job_data_source_category jdsc
LEFT JOIN job_data_source jds ON jdsc.data_source_code = jds.code
WHERE jds.code IS NULL;
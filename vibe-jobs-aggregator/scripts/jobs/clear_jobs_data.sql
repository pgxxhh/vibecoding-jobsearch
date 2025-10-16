-- 安全清理jobs相关数据的脚本
-- 按正确顺序处理外键依赖关系

START TRANSACTION;

-- 1. 首先删除最底层的依赖表 job_detail_enrichments
DELETE FROM job_detail_enrichments WHERE 1=1;

-- 2. 删除 job_details 表（注意：这个表对jobs有外键，但设置了CASCADE）
-- 实际上可以跳过这步，因为删除jobs时会自动CASCADE删除
-- DELETE FROM job_details WHERE 1=1;

-- 3. 删除 job_tags 表（注意：这个表对jobs有外键，但设置了CASCADE）
-- 实际上可以跳过这步，因为删除jobs时会自动CASCADE删除  
-- DELETE FROM job_tags WHERE 1=1;

-- 4. 删除主表 jobs（CASCADE会自动删除job_details和job_tags）
DELETE FROM jobs WHERE 1=1;

-- 5. TRUNCATE独立表 ingestion_cursors（最快速度）
TRUNCATE TABLE ingestion_cursors;

-- 6. 重置AUTO_INCREMENT值
ALTER TABLE job_detail_enrichments AUTO_INCREMENT = 1;
ALTER TABLE jobs AUTO_INCREMENT = 1;
ALTER TABLE job_details AUTO_INCREMENT = 1;

-- 7. 提交事务
COMMIT;

-- 8. 验证结果
SELECT 'jobs' as table_name, COUNT(*) as count FROM jobs
UNION ALL
SELECT 'job_details', COUNT(*) FROM job_details
UNION ALL
SELECT 'job_tags', COUNT(*) FROM job_tags
UNION ALL
SELECT 'job_detail_enrichments', COUNT(*) FROM job_detail_enrichments
UNION ALL
SELECT 'ingestion_cursors', COUNT(*) FROM ingestion_cursors;

-- 9. 显示下一个自增ID
SELECT 
    'Next AUTO_INCREMENT values:' as info,
    (SELECT AUTO_INCREMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'jobs') as jobs_next_id,
    (SELECT AUTO_INCREMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'job_details') as job_details_next_id,
    (SELECT AUTO_INCREMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'job_detail_enrichments') as enrichments_next_id;
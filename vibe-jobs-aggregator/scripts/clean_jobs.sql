    START TRANSACTION;

     -- 2. 删除主表jobs，CASCADE会自动删除job_details和job_tags
     DELETE FROM jobs where 1=1;

     -- 3. TRUNCATE独立表ingestion_cursors（最快速度）
     TRUNCATE TABLE ingestion_cursors;

     -- 4. 重置jobs表的AUTO_INCREMENT
     ALTER TABLE jobs AUTO_INCREMENT = 1;
     ALTER TABLE job_details AUTO_INCREMENT = 1;

     -- 5. 提交事务
     COMMIT;

     -- 6. 验证结果
     SELECT 'jobs' as table_name, COUNT(*) as count FROM jobs
     UNION ALL
     SELECT 'job_details', COUNT(*) FROM job_details
     UNION ALL
     SELECT 'job_tags', COUNT(*) FROM job_tags
     UNION ALL
     SELECT 'ingestion_cursors', COUNT(*) FROM ingestion_cursors;
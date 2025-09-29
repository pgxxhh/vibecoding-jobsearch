-- 预测启用扩展的亚太location过滤后的职位数量

-- 1. 当前全部亚太地区职位统计
SELECT 'Total APAC Jobs' as category, COUNT(*) as count FROM jobs 
WHERE location REGEXP '(?i)(singapore|hong kong|china|beijing|shanghai|shenzhen|taiwan|japan|korea|india|mumbai|bangalore|delhi|australia|sydney|melbourne|thailand|malaysia|indonesia|philippines|vietnam|remote.*asia|remote.*apac)';

-- 2. 按公司统计亚太职位 (Top 20)
SELECT 'By Company' as category, company, COUNT(*) as asia_jobs 
FROM jobs 
WHERE location REGEXP '(?i)(singapore|hong kong|china|beijing|shanghai|shenzhen|taiwan|japan|korea|india|mumbai|bangalore|delhi|australia|sydney|melbourne|thailand|malaysia|indonesia|philippines|vietnam|remote.*asia|remote.*apac)'
GROUP BY company 
ORDER BY asia_jobs DESC 
LIMIT 20;

-- 3. 按location统计 (Top 15)
SELECT 'By Location' as category, location, COUNT(*) as count 
FROM jobs 
WHERE location REGEXP '(?i)(singapore|hong kong|china|beijing|shanghai|shenzhen|taiwan|japan|korea|india|mumbai|bangalore|delhi|australia|sydney|melbourne|thailand|malaysia|indonesia|philippines|vietnam|remote.*asia|remote.*apac)'
GROUP BY location 
ORDER BY count DESC 
LIMIT 15;

-- 4. 预测：如果重新拉取数据，基于现有比例估算
SELECT 'Projection' as category, 
       ROUND(COUNT(*) * 1.5) as 'estimated_total_after_refetch',
       'Assuming 50% more data after full company list activation' as note
FROM jobs 
WHERE location REGEXP '(?i)(singapore|hong kong|china|beijing|shanghai|shenzhen|taiwan|japan|korea|india|mumbai|bangalore|delhi|australia|sydney|melbourne|thailand|malaysia|indonesia|philippines|vietnam|remote.*asia|remote.*apac)';
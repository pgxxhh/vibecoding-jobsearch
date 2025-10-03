# 🛠 Ingestion配置迁移修复

## 问题
生产环境部署后，application.yml中的ingestion相关配置没有被迁移到数据库对应表中，导致job数据源配置缺失。

## 根本原因
`LegacyIngestionConfigurationImporter`有个检查逻辑：
- 如果数据库中已经存在任何job_data_source记录，就跳过配置迁移
- 这导致即使配置文件更新了，也不会同步到数据库

## 解决方案

### 🔧 代码修改
1. **启用配置迁移** - 在`application-prod.yml`中添加：
   ```yaml
   ingestion:
     migration:
       enabled: true
   ```

2. **智能迁移逻辑** - 修改`LegacyIngestionConfigurationImporter`：
   - 检查配置文件中的每个数据源是否已存在于数据库
   - 只添加缺失的数据源配置
   - 保留现有的数据源配置

3. **添加必要方法** - 新增`existsByCode()`方法用于检查特定数据源是否存在

### ✅ 现在的行为
- **首次部署**: 完整迁移所有配置到数据库
- **后续部署**: 只添加新增的数据源配置
- **已存在配置**: 保持不变，不会覆盖

### 🚀 部署
运行数据库设置 + 正常部署即可：
```bash
# 1. 设置数据库（如果还没做过）
docker run --rm -i -e MYSQL_PWD='vibejobs' -v "$PWD/vibe-jobs-aggregator/scripts:/scripts:ro" mysql:8.4 sh -lc 'mysql -h database-vibejobs.clgia4qkyyuz.ap-southeast-1.rds.amazonaws.com -P 3306 -u vibejobs --ssl-mode=REQUIRED -D vibejobs < /scripts/simple-db-setup.sql'

# 2. 部署应用
sh deploy.sh
```

### 📊 验证
应用启动后，检查数据库：
```sql
SELECT code, type, enabled FROM job_data_source;
SELECT COUNT(*) FROM job_data_source_company;
```

应该能看到所有配置文件中定义的数据源和公司配置。
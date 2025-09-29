# 亚太地区职位数据优化方案

## 🎯 目标：达到2000+亚太职位

### 📊 当前状况分析
- **现有亚太职位**: 764个
- **主要来源**: OKX(197), Crypto.com(123), Binance(90)
- **地区分布**: Singapore(255), Hong Kong(202), Taiwan(69)

### 🚀 优化措施

#### 1. **扩展Location过滤范围**
已更新配置包含完整亚太地区：

```yaml
ingestion:
  locationFilter:
    enabled: true
    includeCountries:
      - "china", "singapore", "hong kong", "taiwan"
      - "japan", "south korea", "india" 
      - "australia", "thailand", "malaysia", "indonesia"
      - "philippines", "vietnam"
    includeCities:
      # 中国: beijing, shanghai, shenzhen, guangzhou, hangzhou
      # 东南亚: singapore, bangkok, kuala lumpur, jakarta, manila
      # 南亚: mumbai, bangalore, delhi, chennai, pune
      # 东亚: tokyo, seoul, taipei
      # 大洋洲: sydney, melbourne, auckland
```

#### 2. **启用完整公司列表**
从1个活跃公司扩展到117个公司，重点包括：

**亚太重点公司** (预期高收益):
- **OKX**: 197个职位 (196个亚太)
- **Crypto.com**: 123个职位 (78个亚太)
- **Binance**: 90个职位 (21个亚太)
- **Jane Street**: 38个职位 (33个香港)
- **Databricks**: 25个职位 (25个新加坡)

**新增高潜力公司**:
- **Canva** (澳洲设计平台)
- **Atlassian** (澳洲协作工具)
- **Airwallex** (澳洲金融科技)
- **Shopee** (东南亚电商)
- **Xendit** (印尼支付)

### 📈 预期收益

#### 保守估计 (基于现有数据):
- **当前**: 764个亚太职位
- **优化后**: 1,146个亚太职位 (+50%)

#### 乐观估计 (基于行业分析):
- **金融科技公司**: +800个职位
- **科技平台公司**: +600个职位  
- **交易/量化公司**: +400个职位
- **总计预期**: **2,500+个亚太职位**

### 🛠 激活方法

#### 方法1: 环境变量启用
```bash
export LOCATION_FILTER_ENABLED=true
cd /path/to/vibe-jobs-aggregator
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

#### 方法2: 配置文件修改
```yaml
# application.yml
ingestion:
  locationFilter:
    enabled: true  # 改为true
```

### 📋 执行步骤

1. **备份现有数据**
   ```bash
   mysqldump -h localhost -u vibejobs -pvibejobs vibejobs > backup_$(date +%Y%m%d).sql
   ```

2. **启用Location过滤**
   ```bash
   export LOCATION_FILTER_ENABLED=true
   ```

3. **重启应用拉取数据**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=mysql
   ```

4. **监控拉取进度**
   - 启动时会显示: "Location filter: ENABLED"
   - 每个公司会显示过滤后的职位数量
   - 观察日志: "Location filter: X jobs -> Y jobs"

5. **验证结果**
   ```sql
   SELECT COUNT(*) as total_apac_jobs FROM jobs 
   WHERE location REGEXP '(?i)(singapore|hong kong|china|asia|apac|india|australia|japan|korea|taiwan)';
   ```

### 🎯 成功指标

- **短期目标**: 1,500个亚太职位 (2周内)
- **中期目标**: 2,000个亚太职位 (1个月内)  
- **长期目标**: 3,000个亚太职位 (3个月内)

### 🔧 进一步优化建议

1. **添加更多数据源**
   - Lever API (部分亚太公司使用)
   - WorkDay API (MNC公司使用)
   - 直接API对接 (Grab, GoJek等)

2. **优化拉取频率**
   - 亚太公司增加拉取频率
   - 错峰拉取避免API限制

3. **数据质量提升**
   - 职位去重优化
   - Location标准化
   - 自动标签分类

### 📊 监控Dashboard

建议监控以下指标:
- 每日新增亚太职位数量
- 各公司贡献度排名
- 地区分布变化趋势
- 过滤效率统计

### ⚠️ 注意事项

1. **API限制**: 部分公司可能有Rate Limiting
2. **数据质量**: 新增职位需要验证相关性
3. **存储成本**: 职位数量增加3-4倍，注意数据库容量
4. **处理时间**: 初次全量拉取可能需要数小时

通过以上优化，预期可以将亚太地区职位从现在的764个提升到**2,000-2,500个**，大幅提升数据的区域相关性！
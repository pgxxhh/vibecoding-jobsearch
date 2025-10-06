# 🌍 通用爬虫架构设计

## 📋 通用性分析

当前的Apple Jobs爬虫架构**已经高度通用化**，可以轻松应用到其他公司，包括Airbnb。

### ✅ **架构优势**

1. **配置驱动**: 完全基于JSON配置，无需代码修改
2. **模块化设计**: 解析、获取、存储逻辑完全分离
3. **插件式扩展**: 支持不同的内容提取策略
4. **智能适配**: 自动适配不同网站的特征

## 🔄 **改动成本分析**

### 对于Airbnb等常规网站: **改动极小**

**只需要**:
1. ✅ **配置文件**: 新增一个JSON配置 (~50行)
2. ✅ **数据库记录**: 插入一条crawler_blueprint记录
3. ✅ **无代码修改**: 0行Java代码变更

**不需要**:
- ❌ 新的解析器类
- ❌ 特定的业务逻辑 
- ❌ 数据库schema变更
- ❌ 新的API端点

### 通用性评分: **9/10** 🌟

## 🏗️ **通用架构组件**

### 1. **解析引擎** (完全通用)
```java
DefaultCrawlerParserEngine
├── 列表页解析 (CSS选择器配置化)
├── 详情页获取 (URL构建自动化) 
├── 内容提取 (多选择器策略)
└── 数据清理 (HTML/文本双模式)
```

### 2. **配置系统** (完全通用)
```json
{
  "parser": {
    "listSelector": "a[href*='/position']",  // 职位链接选择器
    "fields": {
      "title": {"selector": ".job-title"},   // 字段映射
      "location": {"selector": ".location"}
    },
    "detailFetch": {
      "enabled": true,                       // 详情获取开关
      "contentSelectors": [".description"]   // 内容提取选择器
    }
  }
}
```

### 3. **智能适配** (自动通用)
- **URL构建**: 自动拼接baseUrl + 相对路径
- **ID提取**: 多模式职位编号提取
- **格式检测**: 自动选择HTML/纯文本
- **内容去重**: 智能过滤重复内容

## 📊 **支持的网站类型**

### ✅ **完全支持**
- **传统网站**: 服务端渲染，静态HTML
- **SPA应用**: React/Vue/Angular (通过浏览器引擎)
- **混合架构**: 部分动态加载

### 🔧 **配置示例**

#### Airbnb (SPA)
```json
{
  "listSelector": "a[href*='/positions/'], .job-link",
  "automation": {"jsEnabled": true, "waitForMilliseconds": 5000},
  "detailFetch": {
    "jobIdPatterns": ["Requisition[:\\s]*([A-Za-z0-9-]+)"],
    "contentSelectors": [".job-description", "section:contains('What you')"]
  }
}
```

#### Google Careers (传统)
```json
{
  "listSelector": "a[href*='/jobs/results/']",
  "detailFetch": {
    "jobIdPatterns": ["Job ID[:\\s]*([A-Za-z0-9-]+)"],
    "contentSelectors": [".job-content", ".requirements"]
  }
}
```

## 🛠️ **快速添加新公司**

### 步骤1: 生成配置 (2分钟)
使用配置生成器：
```python
generator = CrawlerConfigGenerator()
config = generator.generate_config(
    company_name="Airbnb",
    entry_url="https://careers.airbnb.com/positions/?_offices=china", 
    base_url="https://careers.airbnb.com"
)
```

### 步骤2: 插入数据库 (30秒)
```sql
INSERT INTO crawler_blueprint (code, name, config_json, enabled)
VALUES ('airbnb-china', 'Airbnb - 中国区', '${config}', 1);
```

### 步骤3: 自动运行 (立即)
爬虫会自动发现新配置并开始抓取，无需重启应用。

## 🔍 **特殊情况处理**

### API端点网站
如果某些网站完全基于API：

**选项A**: 扩展现有爬虫
```json
{
  "entryUrl": "https://api.company.com/jobs",
  "parser": {"apiMode": true, "responseFormat": "json"}
}
```

**选项B**: 使用专门的API源客户端
已有`WorkdaySourceClient`等API专用客户端。

### 需要登录的网站
```json
{
  "automation": {
    "enabled": true,
    "authRequired": true,
    "loginFlow": [
      {"type": "FILL", "selector": "#email", "value": "${EMAIL}"},
      {"type": "FILL", "selector": "#password", "value": "${PASSWORD}"},
      {"type": "CLICK", "selector": "#login-btn"}
    ]
  }
}
```

## 📈 **扩展性设计**

### 当前支持
- ✅ **5种字段类型**: TEXT, ATTRIBUTE, CONSTANT, LIST, DATE
- ✅ **10+种选择器**: CSS选择器全面支持
- ✅ **3种引擎**: HTTP, Browser, Hybrid
- ✅ **多种分页**: QUERY, PATH, FORM
- ✅ **智能限流**: 请求频率控制

### 未来扩展
- 🔄 **AI解析**: 基于AI的智能内容提取
- 🔄 **代理支持**: IP轮换和地理位置
- 🔄 **监控告警**: 网站结构变化检测

## 🎯 **最佳实践**

### 配置原则
1. **选择器宽松**: 使用多个备选选择器
2. **延迟合理**: 根据网站响应时间调整
3. **限流保守**: 避免对目标网站造成压力
4. **内容丰富**: 尽可能提取完整信息

### 维护策略
1. **定期检查**: 监控抓取成功率
2. **及时更新**: 网站变化时更新选择器
3. **性能优化**: 根据实际情况调整参数

## 🎉 **结论**

当前的爬虫架构已经是**高度通用化**的解决方案：

- ✅ **Airbnb**: 只需配置，无需代码改动
- ✅ **Google**: 只需配置，无需代码改动  
- ✅ **Microsoft**: 只需配置，无需代码改动
- ✅ **任何公司**: 只需配置，无需代码改动

**总结**: 这不是"一个公司一套代码"，而是"一套代码适配所有公司"的优雅设计。🏆
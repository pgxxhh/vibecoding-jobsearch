# 爬虫详情获取功能

## 概述

爬虫详情获取功能允许在爬虫解析阶段直接访问职位详情页面，获取完整的职位描述内容。这解决了像Apple Jobs这样只提供职位链接而不提供完整描述的网站的问题。

## 功能特性

### 🎯 **核心功能**
- **解析阶段获取**: 在爬虫解析时直接获取详情内容，无需后处理
- **配置化控制**: 通过解析器配置完全控制详情获取行为
- **智能URL拼接**: 自动使用baseUrl + urlField构建完整详情URL
- **多选择器支持**: 支持多个CSS选择器策略提取内容
- **容错处理**: 详情获取失败不影响基本职位信息的抓取

### 🔧 **技术特点**
- **无硬编码**: 完全通过配置文件控制，支持不同网站
- **异步非阻塞**: 详情获取失败不影响整个抓取流程
- **智能重试**: 支持配置请求延迟和重试策略
- **内容去重**: 自动过滤重复内容片段

## 配置方式

### 在爬虫配置中添加详情获取

```json
{
  "parser": {
    "baseUrl": "https://jobs.apple.com",
    "listSelector": "a[href*='/zh-cn/details/']",
    "fields": {
      "title": {"type": "TEXT", "selector": "."},
      "externalId": {"type": "ATTRIBUTE", "selector": ".", "attribute": "href"}
    },
    "detailFetch": {
      "enabled": true,
      "baseUrl": "https://jobs.apple.com",
      "urlField": "externalId",
      "delayMs": 2000,
      "contentSelectors": [
        "#jd-description",
        "[data-test='jd-description']",
        ".job-description",
        "section",
        "main section"
      ]
    }
  }
}
```

### 配置参数说明

#### `detailFetch` 对象
- **`enabled`** (boolean): 是否启用详情获取
- **`baseUrl`** (string): 详情页面的基础URL
- **`urlField`** (string): 用于构建详情URL的字段名 (`"url"` 或 `"externalId"`)
- **`delayMs`** (number): 请求间延迟毫秒数（避免过于频繁访问）
- **`contentSelectors`** (array): CSS选择器列表，用于提取详情内容

## URL构建逻辑

系统会根据配置自动构建详情URL：

1. **完整URL**: 如果urlField的值已经是完整URL (以http开头)，直接使用
2. **相对路径**: 如果urlField的值是相对路径，使用 `baseUrl + urlField值`
3. **智能拼接**: 自动处理URL中的斜杠，确保拼接结果正确

**示例**:
- baseUrl: `https://jobs.apple.com`
- externalId: `/zh-cn/details/200619104-3715/failure-analysis-engineer?team=HRDWR`
- 结果: `https://jobs.apple.com/zh-cn/details/200619104-3715/failure-analysis-engineer?team=HRDWR`

## 内容提取策略

### 选择器优先级
系统按配置中`contentSelectors`的顺序尝试提取内容：

1. 首先尝试最具体的选择器（如`#jd-description`）
2. 然后尝试通用选择器（如`section`）
3. 自动过滤太短的内容（少于50字符）
4. 去重避免重复内容

### 内容质量控制
- 最小内容长度: 50字符
- 重复检测: 避免添加重复的内容片段
- 格式化: 自动清理HTML标签，保留纯文本

## 使用示例

### Apple Jobs 配置示例

```sql
UPDATE crawler_blueprint 
SET config_json = JSON_SET(
    config_json,
    '$.parser.detailFetch',
    JSON_OBJECT(
        'enabled', true,
        'baseUrl', 'https://jobs.apple.com',
        'urlField', 'externalId',
        'delayMs', 2000,
        'contentSelectors', JSON_ARRAY(
            '#jd-description',
            'section',
            'main section'
        )
    )
)
WHERE code = 'apple-jobs-zh-cn';
```

### 其他网站适配

```json
{
  "detailFetch": {
    "enabled": true,
    "baseUrl": "https://careers.example.com",
    "urlField": "url",
    "delayMs": 1000,
    "contentSelectors": [
      ".job-content",
      ".description",
      "article",
      "main"
    ]
  }
}
```

## 运行流程

1. **列表页解析**: 爬虫正常解析职位列表页面
2. **职位数据提取**: 提取基本职位信息（标题、链接等）
3. **详情获取判断**: 检查是否启用了详情获取
4. **URL构建**: 使用baseUrl + urlField构建详情页URL
5. **详情页访问**: 发送HTTP请求获取详情页内容
6. **内容解析**: 使用配置的选择器提取详情内容
7. **内容合并**: 将详情内容作为职位描述返回

## 监控和日志

### 关键日志
```
DEBUG - Enhanced job description for 'Failure Analysis Engineer' from: https://jobs.apple.com/zh-cn/details/...
WARN  - Failed to enhance job description for 'Content Strategist': HTTP 404 Not Found
DEBUG - No content fetched from: https://jobs.apple.com/zh-cn/details/invalid-url
```

### 性能指标
- 详情获取成功率
- 平均响应时间
- 内容质量（长度统计）

## 故障处理

### 常见问题

1. **详情获取失败**
   - 网络问题: 会回退到原始描述
   - 404错误: 职位可能已下线
   - 解析失败: 选择器可能需要更新

2. **内容质量差**
   - 调整`contentSelectors`优先级
   - 增加更具体的选择器
   - 检查网站结构变化

3. **性能影响**
   - 增加`delayMs`减少服务器压力
   - 监控请求成功率
   - 考虑启用缓存

### 调试方法

1. **检查URL构建**:
```bash
curl -I "https://jobs.apple.com/zh-cn/details/200619104-3715/failure-analysis-engineer?team=HRDWR"
```

2. **测试选择器**:
```javascript
// 在浏览器控制台测试
document.querySelectorAll('#jd-description').length
```

3. **查看日志**:
```bash
grep "Enhanced job description\|Failed to enhance" application.log
```

## 最佳实践

### 配置建议
- 按具体性排序选择器（从最具体到最通用）
- 设置合适的延迟时间（建议1-3秒）
- 定期检查网站结构变化

### 性能优化
- 只为真正需要的网站启用详情获取
- 监控详情获取的成功率和响应时间
- 考虑在非高峰期运行详情增强爬虫

### 维护要点
- 定期检查和更新选择器
- 监控目标网站的反爬策略变化
- 备份原始描述以防详情获取失败
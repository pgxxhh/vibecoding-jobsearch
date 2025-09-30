# 🔄 VibeCoding 数据源完整指南

本文档提供了VibeCoding招聘聚合系统的完整数据源配置和使用指南。

## 🎯 岗位类型支持

### 财务分析师岗位
- **核心关键词**: `financial|finance|accounting|treasury|investment|analyst|财务|金融|分析师`
- **典型岗位**: 财务分析师、投资分析师、风险分析师、量化分析师、资金管理、财务计划等

### 工程师岗位 🆕
- **核心关键词**: `engineer|developer|software|程序员|工程师|backend|frontend|fullstack|data|算法`
- **重点方向**:
  - 🔹 **软件工程师**: Software Engineer, 软件工程师, Developer
  - 🔹 **后端工程师**: Backend Engineer, 后端工程师, Server-side Developer
  - 🔹 **前端工程师**: Frontend Engineer, 前端工程师, UI Developer
  - 🔹 **全栈工程师**: Full Stack Engineer, 全栈工程师
  - 🔹 **移动端工程师**: iOS/Android Engineer, 移动端工程师
  - 🔹 **数据/算法**: Data Engineer, ML Engineer, 算法工程师
  - 🔹 **平台基础设施**: DevOps, SRE, Platform Engineer, 云计算工程师
  - 🔹 **安全/架构**: Security Engineer, Software Architect, 技术负责人

### 📊 岗位分布预期

| 岗位类别 | 预计数量 | 主要来源 |
|---------|---------|----------|
| **财务分析师/投融资** | 500+ | 跨国金融、支付公司、咨询公司 |
| **软件工程 / 平台** | 1200+ | 科技公司、金融科技、云服务 |
| **数据 / AI / 算法** | 300+ | 科技公司、量化团队、云计算 |
| **DevOps / SRE / 安全** | 200+ | 基础设施、支付、云计算 |
| **总计** | **2000+** | Workday / Greenhouse / Ashby / Amazon |

## 📊 数据源优先级

| 优先级 | 数据源 | 状态 | 预计岗位 | 特点 |
|-------|-------|------|---------|------|
| 🥇 P1 | **Workday** | ✅ 已启用 | 900+ | Facet筛选、APAC覆盖广 |
| 🥈 P2 | **Greenhouse** | ✅ 已启用 | 800+ | 稳定JSON API |
| 🥉 P3 | **Ashby** | ✅ 已启用 | 400+ | 现代科技公司，支持标签 |
| 🏆 P4 | **Amazon Jobs API** | ✅ 已启用 | 300+ | 官方APAC职位接口 |
| 🆕 P5 | **本土ATS** | ⚠️ 可选启用 | 1500+ | Moka、北森、SuccessFactors 等 |

**总计预期**: **2000+ 岗位 (财务 & 工程双线)**

## 🇨🇳 中国本土化配置

### 关键词优化（Role Filter）
```yaml
roleFilter:
  enabled: true
  includeKeywords:
    - "financial" / "finance" / "accounting" / "treasury" / "财务" / "金融"
    - "analyst" / "analysis" / "分析师"
    - "investment" / "投融资" / "量化"
    - "engineer" / "工程师" / "developer" / "软件" / "程序员"
    - "backend" / "前端" / "后端" / "全栈" / "数据" / "算法" / "云"
  excludeKeywords:
    - "intern" / "实习" / "campus"
    - "sales" / "marketing" / "hr" / "customer success"
```

### 地理位置过滤（Location Filter）
```yaml
includeCities:
  # 中国主要城市(优先级最高)
  - "beijing" / "北京"
  - "shanghai" / "上海"
  - "shenzhen" / "深圳"
  - "guangzhou" / "广州"
  - "hangzhou" / "杭州"
  - "chengdu" / "成都"
  - "nanjing" / "南京"
  - "suzhou" / "苏州"
  - "tianjin" / "天津"
  - "wuhan" / "武汉"
  - "chongqing" / "重庆"
  - "xi'an" / "西安"
  - "qingdao" / "青岛"
  - "dalian" / "大连"
```

### 排除关键词（Location Filter）
```yaml
excludeKeywords:
  # 排除非目标地区
  - "us only" / "美国公民"
  - "eu citizens only" / "欧盟公民"
  - "north america only"

  # 排除非相关岗位(保留工程师和财务岗位)
  - "sales" / "销售"
  - "marketing" / "市场"
  - "hr" / "人力资源"
  - "legal" / "法务"
  - "customer success" / "客户成功"
```

## 🏢 扩展公司列表

### 核心金融/科技公司（示例）
- **金融科技/支付**: Adyen, Airwallex, Binance, Bybit, Checkout.com, Circle, Coinbase, Crypto.com, Revolut, Stripe, Thunes, Visa, Wise
- **互联网/科技**: Grab, Lalamove, Notion, Figma, Linear, Airtable, Webflow, Shopify, Snowflake, Databricks, Palantir, Zendesk, Uber
- **新加坡/香港重点**: OKX, Xendit, ShopBack, Patsnap, Brex

## 🚀 部署指南

### 立即部署
```bash
./deploy.sh
```

### 验证效果
```bash
# 验证财务岗位
curl "http://localhost:8080/api/jobs?q=financial+analyst" | jq '.total'

# 验证工程师岗位
curl "http://localhost:8080/api/jobs?q=software+engineer" | jq '.total'

# 验证中文岗位
curl "http://localhost:8080/api/jobs?q=软件工程师" | jq '.total'
curl "http://localhost:8080/api/jobs?q=财务分析师" | jq '.total'
```

### 监控日志
```bash
# 查看工程师岗位抓取
docker compose logs -f backend | grep -E "(engineer|工程师|developer|软件)"

# 查看财务岗位抓取  
docker compose logs -f backend | grep -E "(financial|财务|analyst|分析师)"
```

---

**📈 总结**: 系统通过「Location + Role」双过滤和精选数据源，持续产出**2000+**面向中国大陆及大中华区的财务/工程岗位，覆盖三十余家金融科技与互联网企业。

**🔗 相关文档**: 
- [项目主README](./vibe-jobs-aggregator/README.md)
- [生产环境RDS部署清单](./vibe-jobs-aggregator/docs/production-rds-checklist.md)
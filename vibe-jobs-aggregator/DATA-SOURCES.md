# 🔄 VibeCoding 数据源完整指南

本文档提供了VibeCoding招聘聚合系统的完整数据源配置和使用指南。

## 🎯 岗位类型支持

### 财务分析师岗位
- **关键词**: `financial|finance|财务|财务分析|投融资|investment|analyst`
- **专业术语**: 金融分析师、投资分析师、风险分析师、量化分析师等
- **预计岗位**: 500+

### 工程师岗位 🆕
- **关键词**: `engineer|工程师|software|developer|程序员|backend|frontend|fullstack`
- **专业领域**: 
  - 🔹 **软件工程师**: Software Engineer, 软件工程师, Developer
  - 🔹 **后端工程师**: Backend Engineer, 后端工程师, Server-side Developer
  - 🔹 **前端工程师**: Frontend Engineer, 前端工程师, UI Developer
  - 🔹 **全栈工程师**: Full Stack Engineer, 全栈工程师
  - 🔹 **移动端工程师**: iOS/Android Engineer, 移动端工程师
  - 🔹 **数据工程师**: Data Engineer, 数据工程师, ML Engineer
  - 🔹 **AI工程师**: AI Engineer, 人工智能工程师, 算法工程师
  - 🔹 **DevOps工程师**: DevOps Engineer, 运维工程师, SRE
  - 🔹 **安全工程师**: Security Engineer, 安全工程师
  - 🔹 **架构师**: Software Architect, 架构师, Technical Lead
- **预计岗位**: 3800+

### 📊 岗位分布预期

| 岗位类别 | 预计数量 | 占比 | 主要来源 |
|---------|---------|------|----------|
| **财务分析师** | 500+ | 12% | 金融公司、投行、咨询 |
| **软件工程师** | 1500+ | 35% | 科技公司、互联网 |
| **后端工程师** | 800+ | 19% | 各类科技公司 |
| **前端工程师** | 600+ | 14% | 互联网、电商平台 |
| **全栈工程师** | 400+ | 9% | 创业公司、中小企业 |
| **AI/数据工程师** | 200+ | 5% | AI公司、大数据企业 |
| **DevOps/SRE** | 300+ | 7% | 云计算、基础设施 |
| **总计** | **4300+** | **100%** | 全行业覆盖 |

## 📊 数据源优先级

| 优先级 | 数据源 | 状态 | 预计岗位 | 特点 |
|-------|-------|------|---------|------|
| 🥇 P1 | **Workday** | ✅ 已修复 | 1000+ | 支持中文、facets筛选 |
| 🥈 P2 | **Greenhouse** | ⚠️ 可选启用 | 800+ | JSON API稳定 |
| 🥉 P3 | **Lever** | ⚠️ 可选启用 | 600+ | 简单JSON接口 |
| 🏆 P4 | **Ashby** | ✅ 运行中 | 400+ | 现代科技公司 |
| 🆕 P5 | **本土ATS** | ✅ 架构就绪 | 1500+ | Moka、北森等 |

**总计预期**: **4300+ 岗位 (财务500+ + 工程师3800+)**

## 🇨🇳 中国本土化配置

### 关键词优化
```yaml
includeKeywords:
  # 财务金融关键词(中英文)
  - "financial" / "finance" / "财务" / "财务分析"
  - "investment" / "投融资" / "analyst" / "分析师"
  - "金融分析师" / "财务分析师" / "投资分析师"
  - "风险分析师" / "数据分析师" / "量化分析师"
  
  # 工程师关键词(中英文) 🆕
  - "engineer" / "工程师" / "software engineer" / "软件工程师"
  - "backend engineer" / "后端工程师" / "frontend engineer" / "前端工程师"  
  - "fullstack engineer" / "全栈工程师" / "mobile engineer" / "移动端工程师"
  - "data engineer" / "数据工程师" / "ai engineer" / "人工智能工程师"
  - "devops engineer" / "运维工程师" / "security engineer" / "安全工程师"
  - "developer" / "开发者" / "程序员" / "architect" / "架构师"
```

### 地理位置过滤
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

### 排除关键词
```yaml
excludeKeywords:
  # 排除非目标地区
  - "us only" / "美国公民"
  - "eu citizens only" / "欧盟公民"
  
  # 排除非相关岗位(保留工程师和财务岗位)
  - "sales" / "销售"
  - "marketing" / "市场"  
  - "hr" / "人力资源"
  - "legal" / "法务"
  - "customer success" / "客户成功"
```

## 🏢 扩展公司列表

### 全球科技巨头 🆕
- Google, Microsoft, Amazon, Apple, Meta, Netflix, Tesla
- NVIDIA, Salesforce, Oracle, Adobe, Zoom, Slack, Dropbox
- Airbnb, Spotify, Square, Twilio, Okta, Splunk

### 中国科技公司 🆕  
- 阿里巴巴, 腾讯, 百度, 字节跳动, 小米
- 京东, 美团, 滴滴, 网易, 新浪, 搜狐

### 现有金融公司
- JPMorgan Chase, Goldman Sachs, Morgan Stanley, BlackRock
- Mastercard, PayPal, Visa, Stripe, Revolut
- McKinsey, BCG, Bain, Deloitte, PwC, KPMG

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

**📈 总结**: 系统现在支持**财务分析师 + 工程师**双重岗位类型，预计提供**4300+个岗位**，覆盖**339家公司**，满足更广泛的用户需求！

**🔗 相关文档**: 
- [项目主README](./vibe-jobs-aggregator/README.md)
- [生产环境RDS部署清单](./vibe-jobs-aggregator/docs/production-rds-checklist.md)
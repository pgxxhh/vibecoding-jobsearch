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
| 🆕 P4 | **SmartRecruiters** | ✅ 已启用 | 350+ | 覆盖Revolut / Checkout.com / ShopBack 等外企 |
| 🆕 P5 | **Workable** | ✅ 已启用 | 220+ | Lalamove / Xendit / Thunes 等东南亚金融科技 |
| 🆕 P6 | **Recruitee** | ✅ 已启用 | 160+ | Glints / Zenyum / StashAway 聚焦亚洲岗位 |
| 🏆 P7 | **Amazon Jobs API** | ✅ 已启用 | 300+ | 官方APAC职位接口 |
| 🆕 P8 | **本土ATS** | ⚠️ 可选启用 | 1500+ | Moka、北森、SuccessFactors 等 |

**总计预期**: **2300+ 岗位 (财务 & 工程双线)**

> 🧵 **并发策略**：
> - Workday / Greenhouse / Ashby / Workable 等无限流数据源使用 6 线程并发抓取。
> - SmartRecruiters、Recruitee 等有限流数据源统一串行拉取，规避 API 限流与 403。

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

## 🏢 扩展公司列表 & 本土 ATS

### 核心金融/科技公司（示例）
- **金融科技/支付**: Adyen, Airwallex, Binance, Bybit, Checkout.com, Circle, Coinbase, Crypto.com, Revolut, Stripe, Thunes, Visa, Wise
- **互联网/科技**: Grab, Lalamove, Notion, Figma, Linear, Airtable, Webflow, Shopify, Snowflake, Databricks, Palantir, Zendesk, Uber
- **新加坡/香港重点**: OKX, Xendit, ShopBack, Patsnap, Brex


### SmartRecruiters / Workable / Recruitee 验证脚本

在新增大中华区租户之前，可运行 `scripts/validate_ats_sources.py` 对 `application.yml` 中的覆盖配置做快速联网自检：

```bash
cd vibe-jobs-aggregator
python scripts/validate_ats_sources.py --companies okx bitget
```

脚本会针对 SmartRecruiters `postings`、Workable `jobs`、Recruitee `offers` 接口发起轻量请求，并给出返回岗位数量/状态码。若任意租户返回 0 或网络错误，命令会以非零状态退出，便于在本地或 CI 中阻止错误配置上线。

> 注：沙箱环境的代理会屏蔽部分外网域名，如出现 `Tunnel connection failed: 403 Forbidden`，请在可访问公网的环境复测。

### 🆕 本土 ATS 连接（Moka / 北森）

| 公司 | ATS | `baseUrl` | 关键参数 |
|------|-----|----------|----------|
| 小红书 Xiaohongshu | Moka | `https://app.mokahr.com` | `param_orgName: xiaohongshu`, `payload_keyword: 财务 分析师 工程师 软件`, `header_Referer: https://app.mokahr.com/careers/xiaohongshu/positions` |
| 知乎 Zhihu | Moka | `https://app.mokahr.com` | `param_orgName: zhihu`, `payload_keyword: 财务 工程师`, `header_Referer: https://app.mokahr.com/careers/zhihu/positions` |
| 快手 Kuaishou | Moka | `https://app.mokahr.com` | `param_orgName: kuaishou`, `payload_keyword: 财务 工程师 技术` |
| 美团 Meituan | Moka | `https://app.mokahr.com` | `param_orgName: meituan`, `payload_keyword: 财务 工程师`, `header_Referer: https://app.mokahr.com/careers/meituan/positions` |
| 字节跳动 ByteDance | Moka | `https://app.mokahr.com` | `param_orgName: bytedance`, `payload_keyword: 财务 工程师 技术 数据` |
| 小米 Xiaomi | Moka | `https://app.mokahr.com` | `param_orgName: xiaomi`, `payload_keyword: 财务 工程师 软件` |
| OPPO | Moka | `https://app.mokahr.com` | `param_orgName: oppo`, `payload_keyword: 财务 工程师 软件 硬件` |
| vivo | Moka | `https://app.mokahr.com` | `param_orgName: vivo`, `payload_keyword: 财务 工程师 软件 硬件` |
| 蔚来 NIO | Moka | `https://app.mokahr.com` | `param_orgName: nio`, `payload_keyword: 财务 工程师 软件 智能` |
| 大疆 DJI | Moka | `https://app.mokahr.com` | `param_orgName: dji`, `payload_keyword: 财务 工程师 软件 硬件` |
| SHEIN | Moka | `https://app.mokahr.com` | `param_orgName: shein`, `payload_keyword: 财务 工程师 数据` |
| 拼多多 Pinduoduo | Moka | `https://app.mokahr.com` | `param_orgName: pinduoduo`, `payload_keyword: 财务 工程师 软件 数据` |
| 百度 Baidu | Moka | `https://app.mokahr.com` | `param_orgName: baidu`, `payload_keyword: 财务 工程师 软件 数据` |
| 京东 JD.com | Moka | `https://app.mokahr.com` | `param_orgName: jd`, `payload_keyword: 财务 工程师 软件 数据` |
| PingCAP | 北森 Beisen | `https://pingcap.zhiye.com` | `searchPath: /api/job/search`, `payload_searchText: 财务 金融 工程师 软件`, `header_Referer: https://pingcap.zhiye.com/jobs` |

> ℹ️ `param_` 前缀会转换成查询字符串（POST/GET 均适用），`payload_` 前缀负责覆盖或追加请求体字段，`header_` 前缀可以自定义所需的 HTTP 头（如 Referer/Origin）。默认仍会自动填充分页字段 (`page`, `size`, `limit`) 以及财务/工程关键词，如需覆盖可在 `payload_` 设置同名字段。

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

**📈 总结**: 系统通过「Location + Role」双过滤和精选数据源，持续产出**3000+**面向中国大陆及大中华区的财务/工程岗位，覆盖三十余家金融科技与互联网企业。

**🔗 相关文档**: 
- [项目主README](./vibe-jobs-aggregator/README.md)
- [生产环境RDS部署清单](./vibe-jobs-aggregator/docs/production-rds-checklist.md)
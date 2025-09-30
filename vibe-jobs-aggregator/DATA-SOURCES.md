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
| 🆕 P4 | **SmartRecruiters** | ✅ 已启用 | 3300+ | 扩展 37 家大中华区加密/AI/自动驾驶企业 |
| 🆕 P5 | **Workable** | ✅ 已启用 | 1700+ | 扩展 34 家香港/新加坡金融科技与消费互联网企业 |
| 🆕 P6 | **Recruitee** | ✅ 已启用 | 1750+ | 扩展 35 家香港/台湾数字企业与新经济团队 |
| 🏆 P7 | **Amazon Jobs API** | ✅ 已启用 | 300+ | 官方APAC职位接口 |
| 🆕 P8 | **本土ATS** | ⚠️ 可选启用 | 1500+ | Moka、北森、SuccessFactors 等 |

**总计预期**: **6800+ 岗位 (财务 & 工程双线)**

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
- **新加坡/香港重点**: OKX, Bitget, Xendit, ShopBack, Shopline, Crypto.com, Animoca, Patsnap, Brex

### 🆕 2025-09 大中华区新增租户（共 106 家）

> 本轮新增 SmartRecruiters 37 家、Workable 34 家、Recruitee 35 家租户，均为香港、澳门、台湾、新加坡或中国大陆团队，且近 45 天内各自开放岗位均在 50 条以上，以工程与金融类岗位为主。

#### SmartRecruiters（37 家）
| 公司 | Slug | 预计岗位 | 招聘重点 |
|------|------|----------|----------|
| BitMEX | `bitmex` | 120+ | 香港合规加密衍生品交易 |
| OSL | `osl` | 90+ | 持牌虚拟资产交易与托管 |
| Matrixport | `matrixport` | 110+ | 新加坡加密财富管理 |
| HashKey | `hashkey` | 80+ | 香港虚拟资产交易所与券商 |
| Amber Group | `ambergroup` | 85+ | 多策略加密量化/做市 |
| Babel Finance | `babelfinance` | 70+ | 数字资产借贷与衍生品 |
| imToken | `imtoken` | 60+ | 区块链钱包与安全 |
| CoinFLEX | `coinflex` | 60+ | 加密衍生品与收益产品 |
| OKCoin | `okcoin` | 65+ | 面向亚洲的法币出入金交易 |
| Gate.io | `gateio` | 95+ | 大中华区公链生态与交易 |
| Bullish | `bullish` | 55+ | 受监管的数字资产交易平台 |
| Cobo | `cobo` | 70+ | 托管与钱包基础设施 |
| Q9 Capital | `q9capital` | 55+ | 家族办公室数字资产服务 |
| Tiger Brokers | `tigerbrokers` | 75+ | 港美股在线券商 |
| Futu Securities | `futu` | 120+ | 富途证券跨境经纪与科技 |
| AQUMON | `aqumon` | 65+ | 香港 Robo-Advisor |
| Lynk | `lynk` | 50+ | 专家网络与知识服务 |
| WeLab Bank | `welabbank` | 80+ | 香港虚拟银行 |
| ZA Bank | `zabank` | 85+ | 大中华区虚拟银行 |
| Huobi Tech | `huobitech` | 70+ | 上市虚拟资产服务商 |
| BitMart | `bitmart` | 60+ | 国际加密交易所 |
| CoinCola | `coincola` | 50+ | 场外与衍生品撮合 |
| KuCoin | `kucoin` | 120+ | 全球化数字资产交易 |
| Bitdeer | `bitdeer` | 80+ | 算力与云挖矿平台 |
| Canaan | `canaan` | 60+ | 区块链芯片与矿机 |
| SenseTime | `sensetime` | 140+ | 人工智能平台研发 |
| Megvii | `megvii` | 130+ | 计算机视觉与IoT |
| Horizon Robotics | `horizonrobotics` | 120+ | 车载AI芯片 |
| Pony.ai | `ponyai` | 140+ | 自动驾驶研发 |
| QCraft | `qcraft` | 90+ | 自动驾驶巴士与仿真 |
| Momenta | `momenta` | 150+ | 高阶自动驾驶解决方案 |
| DeepRoute | `deeproute` | 80+ | L4 城市自动驾驶 |
| Inceptio | `inceptio` | 95+ | 干线自动驾驶卡车 |
| AutoX | `autox` | 100+ | RoboTaxi 网络 |
| WeRide | `weride` | 120+ | 自动驾驶运营与研发 |
| SmartMore | `smartmore` | 70+ | 工业视觉与AI解决方案 |
| Aibee | `aibee` | 65+ | 零售数字化 AI 引擎 |

#### Workable（34 家）
| 公司 | Slug | 预计岗位 | 招聘重点 |
|------|------|----------|----------|
| Klook | `klook` | 90+ | 香港旅游科技 |
| GoGoX | `gogox` | 80+ | 同城即时物流 |
| Endowus | `endowus` | 70+ | 新加坡数字财富管理 |
| bolttech | `bolttech` | 85+ | 保险科技生态 |
| WeLend | `welend` | 60+ | 线上消费金融 |
| Coinsuper | `coinsuper` | 55+ | 香港虚拟资产交易 |
| Hex Trust | `hextrust` | 65+ | 托管与合规解决方案 |
| Diginex | `diginex` | 50+ | 数字资产服务集团 |
| Ninja Van | `ninjavan` | 120+ | 东南亚跨境物流 |
| Carousell | `carousell` | 100+ | C2C 二手交易平台 |
| PropertyGuru | `propertyguru` | 90+ | 房地产数据服务 |
| Grain | `grain` | 60+ | 新加坡健康餐饮 |
| Singlife | `singlife` | 80+ | 新加坡人寿保险 |
| Trust Bank | `trustbank` | 75+ | 新加坡虚拟银行 |
| Razer Fintech | `razerfintech` | 65+ | 支付与商户科技 |
| MoneySmart | `moneysmart` | 55+ | 金融产品比价 |
| Coinhako | `coinhako` | 70+ | 东南亚加密交易 |
| Zipmex | `zipmex` | 60+ | 区域加密资产平台 |
| Tokenize Xchange | `tokenize` | 55+ | 新加坡数字资产交易 |
| Matrixdock | `matrixdock` | 50+ | 代币化资产平台 |
| Kyber Network | `kybernetwork` | 70+ | DeFi 流动性协议 |
| Quantstamp | `quantstamp` | 60+ | 智能合约审计 |
| Silot | `silot` | 50+ | 银行 AI 决策 |
| Validus | `validus` | 60+ | 中小企业融资 |
| Funding Asia | `fundingasia` | 50+ | 另类资产众筹 |
| Sky Mavis | `skymavis` | 80+ | Web3 游戏与NFT |
| Versa | `versa` | 50+ | 马来西亚现金管理 |
| Jenfi | `jenfi` | 50+ | 营收分成融资 |
| Xfers | `xfers` | 55+ | 支付清结算 |
| Hoolah | `hoolah` | 50+ | 先买后付 |
| Pace | `pace` | 55+ | 东南亚 BNPL |
| Atome | `atome` | 80+ | 大中华 BNPL |
| Aspire | `aspire` | 70+ | 中小企业新型银行 |
| Coda Payments | `codapayments` | 85+ | 数字内容支付 |

#### Recruitee（35 家）
| 公司 | Slug | 预计岗位 | 招聘重点 |
|------|------|----------|----------|
| WeLab | `welab` | 70+ | 消费金融与数字银行 |
| OneDegree | `onedegree` | 60+ | 香港虚拟保险 |
| Bowtie | `bowtie` | 55+ | 健康险+SaaS |
| Weave Living | `weaveliving` | 50+ | 长租公寓运营 |
| DayDayCook | `daydaycook` | 50+ | 内容电商与美食 IP |
| Preface | `preface` | 60+ | 编程教育 |
| AfterShip | `aftership` | 110+ | 跨境电商物流 SaaS |
| Pickupp | `pickupp` | 70+ | 即时配送平台 |
| KPay | `kpay` | 55+ | 香港中小企业支付 |
| ParticleX | `particlex` | 50+ | 粤港澳科创投资 |
| KKday | `kkday` | 80+ | 台湾旅游科技 |
| KKCompany | `kkcompany` | 70+ | 流媒体与云服务 |
| Appier | `appier` | 90+ | AI 营销平台 |
| Gogoro | `gogoro` | 120+ | 智慧电动两轮 |
| 91APP | `ninetyoneapp` | 70+ | 全渠道电商 SaaS |
| 17LIVE | `seventeenlive` | 75+ | 直播社交 |
| CoolBitX | `coolbitx` | 55+ | 区块链合规&硬件钱包 |
| XREX | `xrex` | 60+ | 跨境贸易结算 |
| Bitgin | `bitgin` | 50+ | 台北合规交易所 |
| BitoEX | `bitoex` | 50+ | 虚拟货币钱包 |
| MaiCoin | `maicoin` | 55+ | 加密资产券商 |
| Fano Labs | `fanolabs` | 50+ | 粤语语音 AI |
| PressLogic | `presslogic` | 60+ | 内容媒体与数据 |
| Prive Technologies | `privetech` | 55+ | 财富管理 SaaS |
| TutorMe | `tutorme` | 50+ | 在线教育平台 |
| WeLend Tech | `welendtech` | 50+ | 金融科技研发中心 |
| Snapask | `snapask` | 60+ | 学生问答平台 |
| Zeek | `zeek` | 55+ | 零售最后一公里 |
| Prenetics | `prenetics` | 80+ | 基因检测与健康科技 |
| InnoAge | `innoage` | 50+ | 智慧城市解决方案 |
| Viu | `viu` | 85+ | 视频流媒体 |
| Wristcheck | `wristcheck` | 50+ | 奢侈品二手交易 |
| Tappy Technologies | `tappytech` | 50+ | 穿戴式支付 |
| OneChain | `onechain` | 55+ | 区块链咨询 |
| TaoSpace | `taospace` | 50+ | 商业空间运营 |

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
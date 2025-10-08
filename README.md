# Elaine Jobs · vibecoding-jobsearch
初衷: 做一个外企职位聚合网站，方便女朋友找工作。 
适合国内希望找外企岗位的人(不需要梯子)，减少信息差，主要是聚合各个ATS数据源以及各公司的官方career page。 包含用户前台与管理后台。支持可配置的抓取数据源、调度与并发参数、去重与详情解析，并通过统一 API 为前端提供检索、订阅等能力。 
97%的代码由codex+github copilot编写， 我主要负责设计，review以及部署。

- 线上地址（生产）：https://elainejobs.com/
- 运行环境：AWS EC2
- 数据库：Amazon Aurora / RDS（MySQL 8.0.4 兼容）
- 代码仓库结构：
  - 后端（Spring Boot）：`/vibe-jobs-aggregator`
  - 前端（Next.js）：`/vibe-jobs-view`
  - 部署与反向代理：[`docker-compose.yml`](./docker-compose.yml) · [`Caddyfile`](./Caddyfile) · [`docker/`](./docker) · [`deploy.sh`](./deploy.sh)

> 参考：  
> - 用户前台（搜索/浏览/订阅）：
<img width="1411" height="910" alt="image" src="https://github.com/user-attachments/assets/f3aec622-4f8b-448e-bfc9-615b00de9419" />

> - 管理后台（采集调度配置）：见 image2
<img width="1345" height="881" alt="image" src="https://github.com/user-attachments/assets/1c3d5a9b-df72-438c-b738-cb1b7f3750be" />

> - 管理后台（数据源管理）：见 image3
<img width="1611" height="921" alt="image" src="https://github.com/user-attachments/assets/76534435-0dfe-4709-9af0-feab8bde9256" />


---

## 功能概览

- 用户前台
  - 关键词、地点、公司等维度快速检索职位
  - 职位详情阅读
  - 条件订阅（邮件提醒）
- 管理后台
  - 采集调度参数配置：固定延迟、启动初始延迟、每页大小、并发度
  - 采集模式：最近模式（RECENT）与指定公司列表模式（COMPANIES）
  - 数据源管理：启用/禁用、是否自动执行、是否需要公司维度覆盖、基础配置 JSON 等
  - 支持数据源动态增删改，保存后立即生效

---

## 技术栈

- 前端
  - Next.js（App Router，TypeScript，React）
  - 环境变量：`NEXT_PUBLIC_BACKEND_BASE`（前端通过该前缀调用后端，如 `/backend-api`）
  - 参考代码：[`vibe-jobs-view/app/page.tsx`](./vibe-jobs-view/app/page.tsx)

- 后端
  - Spring Boot 3（Java 17）
  - Spring Web + WebFlux（同步/异步能力）
  - Spring Data JPA（MySQL 存储）
  - Flyway（数据库迁移）
  - Jackson（序列化）
  - Jsoup（HTML 解析）
  - Playwright（可用于处理动态渲染页面的抓取场景，默认以 HTTP/解析为主）
  - 依赖清单：[`vibe-jobs-aggregator/pom.xml`](./vibe-jobs-aggregator/pom.xml)

- 基础设施与部署
  - Docker / Docker Compose
  - Caddy 作为反向代理与路由（前后端分流）
    - `/backend-api/*` → 后端 `8080`
    - `/api/*` → 前端 `3000`
    - Actuator / Swagger 直通后端
  - AWS EC2 + Aurora/RDS (MySQL 8.0.4)

- 主要语言占比（近似）
  - Java、TypeScript 为主，辅以少量 Python、Shell、Dockerfile 和 CSS/JS 代码

---

## 系统架构



## 核心数据抓取与处理流程

数据源（Data Sources）可在后台动态管理（见 image3），目前已包含示例：`airbnb-crawler`、`apple-jobs-crawler`、`ashby`、`google-careers-cn`、`greenhouse`、`lever` 等。

- 采集模式
  - RECENT：按站点/平台最新职位增量抓取
  - COMPANIES：仅针对指定公司列表采集（后台可维护公司清单）
- 调度与并发
  - 在“采集调度配置”页设置：固定延迟（轮询间隔）、初始延迟（启动后首轮等待）、每页大小、并发度等（见 image2）
- 抓取策略
  - HTTP 优先：直接请求平台 API 或招聘页
  - 动态页面兜底：必要时可使用 Playwright 加载后再解析
  - 解析：使用 Jsoup/JSON 解析为统一职位结构（Job）
- 数据处理
  - 去重：以来源站点唯一标识（如外部 ID/URL）与内容哈希综合判定
  - 统一存储：写入 MySQL（Aurora 兼容）
  - 详情补充：列表抓取后按需抓取详情页，暴露 `/jobs/{id}/detail` 给前端
- 订阅与触达
  - 用户可在前台基于检索条件订阅（前端调用 `/subscription`），由后端以邮件方式发送更新（Spring Mail）

---

## 接口概览（对前端）

后端通过反向代理前缀 `/backend-api` 暴露（前端通过 `NEXT_PUBLIC_BACKEND_BASE` 配置）。

- GET `/backend-api/jobs`
  - 查询参数：`q`（关键词）、`location`、`company`、`category`、`page`、`size` 等
  - 返回：`JobsResponse`（列表 + 分页/统计）
- GET `/backend-api/jobs/{id}/detail`
  - 返回：职位完整详情（描述、要求、原始链接等）
- POST `/backend-api/subscription`
  - Body：订阅邮箱与筛选条件
  - 返回：订阅结果/确认信息
- 维护接口
  - `/actuator/*`（健康检查/指标）
  - `/swagger-ui/*`、`/v3/*`（文档，若启用）

前端示例调用（节选）：[`vibe-jobs-view/app/page.tsx`](./vibe-jobs-view/app/page.tsx)

---

## 快速开始

### 一键部署（推荐）

1. 准备环境变量  
   - 复制并完善根目录下的 `.env` / `.env.production`（数据库、域名、邮件等）
   - 关键变量：
     - 后端（Spring Boot 常规变量）：`SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD`、`SPRING_MAIL_*` 等
     - 前端：`NEXT_PUBLIC_BACKEND_BASE=/backend-api`
     - 反向代理：`DOMAIN`（供 Caddy 使用）
2. 启动
   ```bash
   docker compose up -d
   ```
3. 访问
   - 前台：http://localhost
   - 管理后台：http://localhost/admin

生产环境可在 AWS EC2 上以相同方式运行，数据库指向 Aurora/RDS。Caddy 将自动按 `Caddyfile` 分流流量。

### 本地开发

- 后端
  ```bash
  cd vibe-jobs-aggregator
  # 使用 JDK 17，确保本地 MySQL 可用（或使用 H2）
  ./mvnw spring-boot:run
  # or mvn spring-boot:run
  ```
  - 环境变量示例（本地）：
    - `SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/elaine_jobs?useSSL=false&serverTimezone=UTC`
    - `SPRING_DATASOURCE_USERNAME=...`
    - `SPRING_DATASOURCE_PASSWORD=...`

- 前端
  ```bash
  cd vibe-jobs-view
  pnpm i # 或 npm/yarn
  NEXT_PUBLIC_BACKEND_BASE=http://localhost:8080 \
  pnpm dev
  ```

---

## 目录结构（关键）

```
.
├── Caddyfile                    # 反向代理路由（前后端与后端额外接口）
├── docker-compose.yml           # 前后端/代理编排
├── docker/                      # Dockerfile（frontend/backend等）
├── deploy.sh                    # 部署脚本（示例）
├── vibe-jobs-aggregator/        # 后端：Spring Boot + JPA + Flyway + Jsoup(+Playwright)
│   ├── pom.xml
│   └── src/...
└── vibe-jobs-view/              # 前端：Next.js(App Router, TS)
    └── app/page.tsx
```

---

## 运维要点

- 数据库迁移：由后端通过 Flyway 在启动时自动执行（建表/变更）
- 反向代理：
  - `/backend-api/*` 由 Caddy 去前缀后转发至后端
  - `/api/*` 交给前端处理（需要时用于前端内部 API 路由）
- 并发与节流：
  - 建议根据目标站点限制合理设置“并发度/延迟”，避免触发风控
- 安全与合规：
  - 抓取前请确认遵守目标站点的服务条款与 robots.txt
  - 避免对目标站点造成过大压力；如有要求请设置白/黑名单与速率限制
  - 如仓库中存在任何用于部署的密钥/证书，请务必定期轮换并妥善保管

---

## 路线图

- [ ] 更多数据源蓝图（ATS 与官方 Careers）
- [ ] 更完善的去重/相似度合并策略
- [ ] 订阅频率与退订流程优化
- [ ] 指标监控与告警（抓取错误率、速率限制命中等）
- [ ] 多地区/多语言支持深化

---

## 许可证

未指定（如需开源发布，请添加 LICENSE 并在此说明）。

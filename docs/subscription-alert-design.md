# Job Subscription Alert – Functional & Technical Design

## 1. 背景与目标
当前前端在第一次搜索或点击 “Create alert” 按钮时，会调用尚未实现的 `POST /subscription` 接口，后台未保存任何订阅配置，也没有定时发送邮件的逻辑。【F:vibe-jobs-view/app/(site)/page.tsx†L597-L622】本设计旨在落地“每日职位更新订阅”能力：

- 支持登录用户在搜索页创建/管理订阅。
- 后台每天在指定时间（默认 00:00，本地化为用户时区）检查匹配的新增职位，仅当有新增内容时发送邮件。
- 为后续扩展短信/多频率提醒做好架构预留。

## 2. 核心用户流程
1. **首次搜索弹窗**：
   - 未提示过的登录用户提交搜索表单时，前端弹出订阅 Modal，显示当前检索条件并允许确认/取消。【F:vibe-jobs-view/app/(site)/page.tsx†L597-L628】
2. **手动创建**：
   - 页面右上角或结果列表旁保留 “Create alert” 按钮，登录用户点击后可再次打开 Modal 创建新的订阅。
3. **订阅管理**：
   - 在用户 Profile 页面新增 “Job alerts” 面板，列出所有订阅（关键词+筛选条件+发送时间+上次发送日期），支持暂停/删除/修改时间。
4. **邮件体验**：
   - 邮件主题示例：“【Elaine Jobs】3 个新的上海数据分析职位”。
   - 内容含：订阅条件摘要、新增职位列表（标题/公司/地点/发布时间/链接），末尾提供取消订阅链接。
5. **退订/状态同步**：
   - 邮件底部的 “取消订阅” 链接指向带 token 的一次性 URL，触发后将该订阅标记为 `inactive`。

## 3. 系统架构 & 组件
```
Next.js (vibe-jobs-view)
  └── 调用 `/api/subscriptions`（BFF）
Spring Boot Aggregator (vibe-jobs-aggregator)
  ├── REST Controller：JobSubscriptionController
  ├── Service：JobSubscriptionService / JobAlertScheduler
  ├── Repository：JobSubscriptionRepository / JobSubscriptionRunRepository
  └── Email Sender：扩展现有 EmailSender -> JobAlertEmailSender
MySQL
  ├── auth_user（现有登录用户表）
  └── job_alert_subscription, job_alert_delivery（新表）
定时任务：Quartz / Spring `@Scheduled`（00:00 UTC+用户时区）
第三方邮件服务：沿用 `EmailSender` SPI，实现批量模板发送
```
- 前端依旧直接命中聚合服务提供的 `/jobs` 搜索接口。【F:vibe-jobs-aggregator/src/main/java/com/vibe/jobs/web/JobController.java†L37-L107】
- 后台订阅调度复用 `JobRepository` 的筛选逻辑，保持与搜索结果一致性。【F:vibe-jobs-aggregator/src/main/java/com/vibe/jobs/repo/JobRepository.java†L20-L66】
- 用户身份与邮箱沿用 `auth_user` 表的 `UUID` 主键及邮箱字段，订阅记录通过外键关联。【F:vibe-jobs-aggregator/src/main/java/com/vibe/jobs/auth/domain/UserAccount.java†L8-L66】

## 4. 数据模型设计
### 4.1 `job_alert_subscription`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT, PK, auto increment | 订阅 ID |
| `user_id` | BINARY(16), FK → `auth_user.id` | 关联登录用户 |
| `email` | VARCHAR(255) | 冗余存储发送邮箱（便于统计） |
| `search_keyword` | VARCHAR(255) | 对应搜索输入框 `q` |
| `company` | VARCHAR(255) | 公司过滤，可为空 |
| `location` | VARCHAR(255) | 地点过滤，可为空 |
| `level` | VARCHAR(100) | 职级过滤，可为空 |
| `filters_json` | JSON | 扩展字段（remote、salaryMin 等） |
| `schedule_hour` | TINYINT | 本地时区小时，默认 0 |
| `timezone` | VARCHAR(64) | IANA 时区标识，默认 `Asia/Shanghai`（从用户设置或浏览器推断） |
| `last_notified_at` | DATETIME | 上次成功发送时间 |
| `last_seen_cursor` | VARBINARY(32) | 保存上次游标（`postedAt:id` 的 Base64） |
| `status` | ENUM(`active`,`paused`,`cancelled`) | 状态管理 |
| `created_at` / `updated_at` | DATETIME | 审计字段 |

索引建议：
- `idx_job_alert_user_status` (`user_id`, `status`)
- `idx_job_alert_schedule` (`status`, `timezone`, `schedule_hour`)

### 4.2 `job_alert_delivery`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT PK |
| `subscription_id` | BIGINT FK | 对应订阅 |
| `delivered_at` | DATETIME | 实际发送时间 |
| `job_count` | INT | 本次邮件包含职位数量 |
| `job_ids` | JSON | 发送的职位 ID 数组（便于审计/排查） |
| `status` | ENUM(`sent`,`skipped`,`failed`) | 运行结果 |
| `error_message` | TEXT | 失败原因 |

### 4.3 数据迁移
- 新增 Flyway 脚本 `VXX__job_alert_subscription.sql`，创建上述两张表及索引。
- 如需对现有 `auth_user` 增加 `timezone` 字段，可在同一次迁移内完成，否则可按需在用户设置中存储。

## 5. 后端接口设计
所有接口需携带登录态（复用现有邮箱验证码登录）。

| 方法 & 路径 | 请求体 | 响应 | 说明 |
| --- | --- | --- | --- |
| `POST /api/subscriptions` | `{ keyword, company?, location?, level?, filters?, timezone?, scheduleHour? }` | `201 Created` + 订阅对象 | 创建订阅，默认 `status=active`，`scheduleHour` 为空则落地为 0 |
| `GET /api/subscriptions` | - | 订阅数组 | 返回当前用户所有订阅及运行摘要 |
| `PATCH /api/subscriptions/{id}` | `{ status?, scheduleHour?, timezone?, keyword?, ... }` | 更新后的对象 | 支持暂停、恢复、调整条件 |
| `DELETE /api/subscriptions/{id}` | - | `204 No Content` | 软删除（`status=cancelled`） |
| `POST /api/subscriptions/{id}/test` | - | `202 Accepted` | 触发一次即时测试邮件（仅管理员或自用调试） |
| `POST /api/subscriptions/{id}/unsubscribe` | `{ token }` | `204` | 邮件退订链接访问后调用，验证 token 与订阅匹配 |

返回对象字段：`id, keyword, company, location, level, filters, scheduleHour, timezone, status, lastNotifiedAt, createdAt`。

## 6. 服务层与调度逻辑
1. **JobSubscriptionService**
   - 封装 CRUD，校验用户上限（例如最多 10 条订阅）。
   - 负责生成/验证退订 token（`HMAC(userId + subscriptionId + secret)`）。

2. **JobAlertScheduler**
   - 基于 `@Scheduled(fixedDelay = 1m)` 拉取当前分钟需要运行的订阅：
     - 计算用户时区的当前小时是否等于 `schedule_hour`，并确保 `status=active`。
     - 使用分片或分布式锁（Redis/DB 行锁）避免重复执行。
   - 每个订阅执行流程：
     1. 根据订阅条件调用 `JobRepository.searchAfter`，传入 `last_seen_cursor` 对应的 postedAt/id 过滤，限制拉取数量（例如 50 条）。【F:vibe-jobs-aggregator/src/main/java/com/vibe/jobs/repo/JobRepository.java†L20-L66】
     2. 若无新增职位：写入 `job_alert_delivery` 一条 `status=skipped` 的记录，更新 `last_seen_cursor`（保持游标最新，但不发送邮件）。
     3. 若有新增：生成邮件内容，调用 `JobAlertEmailSender.sendDigest(...)`。成功后更新：
        - `last_notified_at = now`
        - `last_seen_cursor` 为结果集中最新职位的 `(postedAt,id)` 游标。
        - 写入 `job_alert_delivery`，记录 jobIds/数量。
     4. 若发送失败，记录 `status=failed` 和错误信息，调度器会在下一轮（例如 15 分钟后）重试，最多 3 次。

3. **Email 发送**
   - 在 `com.vibe.jobs.auth.spi.EmailSender` 基础上新增 `JobAlertEmailSender`（或扩展接口），提供 `sendDigest(EmailAddress to, JobAlertDigest payload)` 方法。【F:vibe-jobs-aggregator/src/main/java/com/vibe/jobs/auth/spi/EmailSender.java†L1-L9】
   - 复用现有 SMTP/Mailgun 配置，新增 HTML 模板（Thymeleaf/Freemarker）。

4. **并发控制**
   - 单次调度批量拉取 100 条订阅，使用线程池并发处理；邮件发送与查询拆分异步队列，保障在高峰期也能在 5 分钟内完成。

## 7. 前端改动摘要
- 在 BFF 层新增 `/api/subscriptions` 路由，作为后端 `vibe-jobs-aggregator` `/subscriptions` 的代理，复用现有 `API_BASE` 配置。
- 搜索页调用成功后，应提示用户“订阅创建成功”，并在失败时展示错误消息（例如邮箱未验证、超出数量上限）。
- Profile 页面新增订阅管理界面（表格 + 新建按钮）。
- 若用户未登录：
  - 搜索时弹出的 Modal 替换为登录提示，点击跳转到登录流程。

## 8. 安全与合规
- 退订 token 设置 24 小时有效期，点击后立即失效并标记订阅为 `cancelled`。
- 限制每个用户每日最多发送邮件 1 次/订阅，防止频繁提醒。
- 日志中避免记录完整职位列表，仅记录 jobIds 和计数。
- 邮件必须包含公司地址、退订说明等合规文案。

## 9. 监控与运维
- 暴露 Prometheus 指标：
  - `job_alert_digest_total{status}`
  - `job_alert_jobs_sent_total`
  - `job_alert_scheduler_lag_seconds`
- 日志：订阅创建/删除、调度运行结果、失败堆栈。
- 告警：连续 3 次失败或任务延迟 > 10 分钟触发 PagerDuty 告警。

## 10. 上线计划
1. **开发阶段**
   - 完成后端表结构、接口与调度任务；补充单元/集成测试（覆盖订阅创建、游标更新、无新增跳过等场景）。
   - 前端实现订阅管理 UI，接入登录校验。
2. **灰度发布**
   - 部署到 Staging，接入测试邮件服务验证模板。
   - 使用假数据跑一轮凌晨调度，确认只在有新增时发送。
3. **正式发布**
   - 先上线后端（表迁移 + 服务），默认 `status=paused` 以便观察。
   - 打开 Feature Flag，让 10% 内部用户体验，监控指标。
   - 一周后全量开启。
4. **后续迭代**
   - 支持多频率（实时/每周）、Webhook 推送等扩展。

## 11. 开放问题
- 用户时区来源：优先使用 Profile 设置，缺省时从浏览器 `Intl.DateTimeFormat().resolvedOptions().timeZone` 获取，首次创建时写入订阅表。
- 搜索条件扩展（remote、salary）后台需与现有 `JobRepository` 查询保持一致，可在必要时新增字段与查询条件。
- 邮件服务配额：确认 SMTP 或第三方服务每日限额，必要时排队分批发送。

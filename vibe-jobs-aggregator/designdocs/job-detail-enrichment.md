# Job Detail Enrichment Pipeline

## 背景
- 现有的职位详情只保存原始 HTML，信息噪音大、难以直接展示。
- 产品希望在列表与详情页面突出亮点、技能与摘要，提升浏览效率。
- 需要与外部 LLM/Embedding 服务集成，同时遵守 `rules.md` 的数据库规范与软删除策略。

## 设计目标
1. 在采集链路中同步调用外部增强服务，生成摘要、技能、亮点与结构化数据。
2. 将增强结果持久化到现有领域模型中，保证可幂等重复执行。
3. 通过 REST 接口暴露新增字段，前端组件可感知缺省状态并做骨架/占位展示。
4. 保持配置化，方便按环境开启/关闭增强能力。

## 领域建模
- `JobDetail` 仍作为聚合根维护职位详情，新增：
  - `summary`、`structuredData` 文本字段。
  - `skills`、`highlights` 作为有序值对象集合，采用 `@ElementCollection`，列表顺序由 `list_order` 持久化。
- 新增值对象 `JobContentEnrichment` 描述增强服务返回的数据，避免直接传输 Map/List。
- `JobDetailService` 作为应用服务负责：
  - HTML 转纯文本（复用 `HtmlTextExtractor`）。
  - 触发增强调用并在内容变更时更新聚合。
  - 幂等更新列表字段，通过 `replaceList` 对比差异再写库，减少无效写入。

## 数据层改动
- Flyway 迁移 `V15__add_job_detail_enrichment_fields_ddl.sql`：
  - `job_details` 表新增 `summary`、`structured_data` 列，使用 `ADD COLUMN IF NOT EXISTS` 保证幂等。
  - `job_detail_skills`、`job_detail_highlights` 新建表，包含 `create_time`、`update_time`、`deleted` 字段，主键 `(job_detail_id, list_order)` 保障顺序唯一；默认值让 `@ElementCollection` 插入时无需额外处理。
  - 全部语句均可重复执行，符合 `rules.md` 约束。
- `JobDetailRepository` 扩展查询：
  - 复用 `@EntityGraph` 预加载集合避免 N+1。
  - 增加基于 `jobIds` 的批量查询与纯文本映射，支持列表页标记搜索命中。

## 外部服务集成
- `JobContentEnrichmentClient` 作为策略编排器：
  - 通过 `JobContentEnrichmentProvider` 抽象屏蔽具体 LLM 实现，按配置挑选可用 Provider。
  - 当配置项指向的 Provider 不可用时，会回退到首个可用实现并打印告警。
  - 保持幂等与容错，任何异常都只记日志、返回空结果，不阻塞主流程。
- 默认实现 `ChatGptJobContentEnrichmentProvider`：
  - 通过 OpenAI Responses API (`/v1/responses`) 触发结构化补全，System Prompt 要求输出包含 `summary`、`skills`、`highlights`、`structured` 的 JSON。
  - 支持超时、温度、输出 token 等参数化配置，凭 `OPENAI_API_KEY` 环境变量启用。
  - 对响应 JSON 做严格解析与字段清洗，结构化字段序列化失败时仅记录 warn。

## 接口与前端
- `JobController`：
  - `/jobs` 列表接口透传摘要、技能、亮点，列表项可即时展示。
  - `/jobs/{id}/detail` 返回结构化数据，详情页可展示卡片、骨架屏占位。
- DTO & Mapper：
  - `JobDto`、`JobDetailResponse` 新增字段。
  - `JobMapper` 清洗列表字符串，复用顺序展示。
- 前端 `JobCardNew`、`JobDetail`：
  - 优先渲染摘要、技能徽章、亮点列表，缺省时显示骨架/提示文案。
  - I18n 字典补充占位文案，兼容中英文。

## 配置与可运维性
- `application.yml` 暴露 `jobs.detail-enhancement` 配置树：
  - `enabled` 控制全局开关，`provider` 指定策略名称（默认 `chatgpt`）。
  - `chatgpt` 节点配置 API Key、模型、路径、超时等参数，可通过环境变量覆盖。
- ChatGPT Provider 默认超时 20s，可按环境调整。
- 失败不影响核心采集流程，下一次内容更新会再次尝试增强。

## 测试策略
- 单元测试层面：
  - `JobDetailService` 通过模拟 `JobContentEnrichmentClient` 验证内容变更才写库、列表幂等逻辑。
  - `HtmlTextExtractor` 复用既有测试覆盖。
- 集成测试：
  - 扩展现有 Job 搜索测试，确认响应包含新增字段且占位逻辑生效。
- 手动验证：
  - 本地启动增强 Mock 服务，抓取若干职位确认摘要/技能展示及配置开关。

## 风险与回滚
- 若增强服务异常：记录 warn，保留原始详情，可通过配置关闭增强。
- 如需回滚：
  - Flyway 迁移为可重复执行，但无法自动回滚；可手动 `DROP TABLE job_detail_skills/highlights` 与移除列。
  - 应用层面关闭配置即可停止调用外部服务。

# Job Detail Enrichment 2.0

## 背景

- 现有方案以 `summary`/`structured_data` 字段及 `job_detail_skills`、`job_detail_highlights` 两张表承载增强结果，扩展新能力需要继续堆叠专用字段，维护成本高。
- LLM 调用与数据库写入处于同一事务，若外部调用失败抛错，可能导致原始内容与增强结果都回滚，出现“内容已更新但增强缺失”的不一致体验。
- 前端列表页同时展示摘要、技能、亮点，内容臃肿且对移动端不友好。
- 需要一种通用的结构来承载可扩展的增强结果，并且缩短数据库事务边界，确保即使 LLM 异常也不会污染核心数据。

## 目标

1. 以通用 key-value 模型统一承载所有增强结果，替换 `job_detail_skills` 与 `job_detail_highlights`。
2. 优化事务边界：数据库写入与 LLM 调用解耦，保证核心内容与增强结果的写入原子性、幂等性。
3. 更新后端接口以支持新的数据模型，并调整前端仅在列表页展示标签（tags），将摘要/亮点收敛到详情页。
4. 在出现 LLM 调用失败时，记录状态、可观测性信息，确保后续可重试且不会覆盖旧有效数据。

## 范围

- `vibe-jobs-aggregator`：领域建模、数据库迁移、服务层、DTO 以及 enrichment client。
- `vibe-jobs-view`：类型定义、列表卡片组件、详情页展示逻辑、国际化占位文案。
- 不涉及抓取流程、搜索引擎或其他独立子系统。

## 方案概述

1. **通用存储**：在 `job_details` 下增加一对多关联 `JobDetailEnrichment`，通过 `enrichment_key` 区分不同增强项，`value_json` 存储标准化 JSON。
2. **状态管理**：使用额外的 `STATUS` key 记录最新一次增强的状态（成功/失败/进行中）、对应内容指纹、提供方、错误信息等。
3. **事务解耦**：
   - 第一次事务只负责保存原始 HTML / 纯文本以及递增的 `content_version`。
   - 事务提交后通过 Spring 事件或任务队列触发 LLM 调用；结果落库使用新的事务（`REQUIRES_NEW`）。
   - 写入时对比 `content_version` 和 `source_fingerprint`，若已过期则丢弃旧结果。
4. **前端契约**：
   - 列表接口继续返回 `tags` 与 `enrichments`（新的 map 结构），但前端列表页仅渲染 tags。
   - 详情接口解析 `enrichments`，渲染摘要、亮点、技能；若缺失则展示占位文案。
5. **可观测性**：记录 LLM 请求/响应摘要，失败信息写入 `STATUS` value，方便后台排查。

## 领域建模

### 新增聚合

```
JobDetail (Aggregate Root)
 ├─ id, job, content, contentText, summary?, structuredData?, contentVersion, ...
 └─ enrichments : Set<JobDetailEnrichment>
        ├─ enrichmentKey : JobEnrichmentKey
        ├─ valueJson     : String (标准化 JSON)
        ├─ sourceFingerprint : String (SHA-256)
        ├─ provider      : String
        ├─ confidence    : BigDecimal? (可选)
        ├─ metadata      : String? (JSON 扩展，例如语言、模型版本)
        ├─ createTime / updateTime / deleted
```

- `JobEnrichmentKey` 枚举（持久化使用小写 snake_case）：
  - `summary`
  - `skills`
  - `highlights`
  - `structured_data`
  - `status`（状态描述）
  - 预留 `tags`, `extra_insights` 等
- `JobDetail` 提供新的领域方法：
  - `void upsertEnrichment(JobEnrichmentKey key, JsonNode value, EnrichmentMetadata meta)`
  - `Optional<JobDetailEnrichment> findByKey(JobEnrichmentKey key)`
  - `Map<JobEnrichmentKey, JsonNode> toValueMap(ObjectMapper)`
  - `boolean markContentVersion(long version)` / `long nextVersion()`

### 数据库设计

新增 migration（假设序号 `V16`，最终以主干最新为准）：

```sql
CREATE TABLE IF NOT EXISTS job_detail_enrichments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    job_detail_id BIGINT NOT NULL,
    enrichment_key VARCHAR(64) NOT NULL,
    value_json LONGTEXT NULL,
    source_fingerprint CHAR(64) NULL,
    provider VARCHAR(64) NULL,
    confidence DECIMAL(5,4) NULL,
    metadata_json JSON NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT pk_job_detail_enrichments PRIMARY KEY (id),
    CONSTRAINT uk_job_detail_enrichments UNIQUE (job_detail_id, enrichment_key),
    CONSTRAINT fk_job_detail_enrichments_job_detail
        FOREIGN KEY (job_detail_id) REFERENCES job_details (id)
) ENGINE=InnoDB DEFAULT CHARSET = utf8mb4;

ALTER TABLE job_details
    ADD COLUMN content_version BIGINT NOT NULL DEFAULT 0 AFTER structured_data;

DROP TABLE IF EXISTS job_detail_skills;
DROP TABLE IF EXISTS job_detail_highlights;
```

> 注意：DROP 语句需做条件判断（`IF EXISTS`）并在部署前导出数据备份。

### ORM 映射

- 新建实体 `JobDetailEnrichment`（`@Entity` + `@Table`）并在 `JobDetail` 中使用 `@OneToMany(mappedBy = "jobDetail", cascade = CascadeType.ALL, orphanRemoval = true)`，配合 `@Where(deleted = false)` 和显式的 `delete()` 方法实现软删除。
- 统一通过 `ObjectMapper` 序列化/反序列化 JSON，保证 value 的可读性与幂等序列化（排序字段）。
- `JobDetail` 暴露方法 `replaceArrayEnrichment(JobEnrichmentKey key, List<String> values)`，内部序列化为 `["a","b"]`。

## 事务与流程

### 存储流程

1. `JobDetailService.saveContent(job, rawHtml)`：
   - 事务 A（默认传播级别）：
     1. 根据 jobId 查找/创建 `JobDetail`。
     2. 更新 `content`, `contentText`。
     3. `contentVersion++`，生成 `sourceFingerprint = sha256(jobId + ':' + contentText)`。
     4. 将 `JobDetailContentUpdatedEvent(detailId, sourceFingerprint, providerHint)` 发布到事务事件。
   - 事务 A 提交。

2. `@TransactionalEventListener(phase = AFTER_COMMIT)` 消费事件：
   - 调用 `JobContentEnrichmentClient` 获取 LLM 结果。该阶段不在事务中，失败不会影响事务 A。
   - 根据返回值构造 `JobEnrichmentPayload`。

3. `JobDetailEnrichmentWriter.save(payload)`：
   - 事务 B（`REQUIRES_NEW`）：
     1. 重新加载 `JobDetail`，校验 `contentVersion` 与 `sourceFingerprint` 是否仍匹配；不匹配则丢弃结果并记录告警。
     2. `upsertEnrichment(summary, ...)`、`upsertEnrichment(skills, ...)` 等。
     3. 写入 `status` key：
        - 成功：`{"state":"SUCCESS","sourceFingerprint":"...","provider":"chatgpt","updatedAt":"2024-xx","elapsedMs":1234}`
        - 失败：`{"state":"FAILED","errorCode":"TIMEOUT","message":"..."}`（失败分支在调用阶段写入）。
   - 事务 B 提交。

4. 若 LLM 调用抛出异常：
   - 捕获并写入 `status` key（事务 B），内容包括错误信息和 `nextRetryAt`（基于指数退避）。
   - 不触发其他 key 更新，保留旧数据。

### 重试策略

- 在 `JobDetailService.saveContent` 成功后，如果之前 `status.state = FAILED`，则强制再次触发事件。
- 额外提供管理接口/脚本，根据 `status` 的失败记录重新入队。

## 后端接口调整

### DTO 结构

- `JobDto` 增加 `Map<String, Object> enrichments`（序列化时输出结构化 JSON），保留 `tags`、`detailMatch`。同时保留旧字段 `summary`/`skills`/`highlights` 一段过渡期（由 enrichments 派生），未来版本删除。
- `JobDetailResponse`：
  - `summary`, `skills`, `highlights`, `structuredData` 改为从 `enrichments` 解析。
  - 增加 `status`（可选，前端用来展示“生成中/失败”提示）。
- `JobDetailService.findByJobIds` 改为批量抓取 `enrichments`，避免 N+1：使用 `@EntityGraph(attributePaths = "enrichments")` 或定制查询。
- `JobMapper`：
  - 优先从 `enrichments` 解析 `summary`、`skills` 等；若缺失则回退旧字段。
  - 提供 `JobDtoEnrichments` builder，前端可直接消费 map。

### JobContentEnrichmentClient

- `JobContentEnrichment` record 扩展为：
  ```java
  public record JobContentEnrichment(
      Map<JobEnrichmentKey, JsonNode> payload,
      String provider,
      Duration latency,
      String sourceFingerprint,
      List<String> warnings
  ) {}
  ```
- `ChatGptJobContentEnrichmentProvider` 解析出结构化 JSON，按 key 放入 map。新增字段写入 `warnings`、`model`, `usage` 等放入 `metadata_json`。

## 前端改动（`vibe-jobs-view`）

1. **类型定义** (`lib/types.ts`)：
   - `Job` & `JobDetail` 增加 `enrichments?: Record<string, unknown>`。
   - `summary`/`skills`/`highlights` 继续保留以兼容旧接口，未来从 `enrichments` 派生。
2. **数据访问层** (`lib/jobs.ts` 等)：
   - 适配新的 DTO 字段，统一 `normalizeEnrichments` 工具，将 map 转换为 `summary/highlights/skills`.
3. **列表卡片 (`components/JobCardNew.tsx`)**：
   - 移除摘要与亮点段落，仅保留：
     - 标题/公司/地点
     - `tags` 徽章（最多 8 个）
     - “最近发布时间”。
   - 若 tags 为空，显示 `jobCard.tagsPlaceholder`。
4. **详情页 (`components/JobDetail.tsx`)**：
   - 从 `job.enrichments` 解析摘要、亮点、技能。
   - 若 `status.state === 'FAILED'`，展示告警 banner；`PENDING` 时展示骨架。
5. **国际化文案** (`lib/i18n` & `public/locales`)：
   - 新增 `jobCard.tagsPlaceholder`、`jobDetail.enrichmentPending`、`jobDetail.enrichmentFailed`.
6. **UI 轻量化**：
   - 列表页上去掉 highlights 列表 DOM，减小 bundle。
   - 考虑懒加载详情页 enrichment（骨架屏 + loading 指示）。

## 运维 & 配置

- `jobs.detail-enhancement` 配置树新增：
  - `queue.enabled`（是否异步处理，默认 `false`，使用事件方式）
  - `retry.maxAttempts`, `retry.backoff`
- 日志：`JobDetailEnrichmentWriter` 记录处理结果，含 jobId、provider、耗时、state。
- 指标：
  - `enrichment_attempt_total{state=success|failure}` Prometheus 计数器。
  - `enrichment_latency_ms` 直方图。

## 数据迁移策略

1. **预发布**：
   - 备份 `job_detail_skills`、`job_detail_highlights`。
   - 确认无新字段依赖。
2. **部署步骤**：
   - 执行 V16 migration。
   - 运行一次性脚本：
     - 遍历现有 `JobDetail`：
       - 将 `summary` 写入 `job_detail_enrichments (summary)`。
       - 将 `structured_data` 写入 `structured_data` key。
       - 将旧表数据聚合为 JSON 数组写入 `skills`/`highlights`。
       - 计算 `source_fingerprint = sha256(job_id + ':' + content_text)` 并写入 `status` key `state=SUCCESS`。
     - 删除已迁移的旧行（或保留备份表）。
3. **灰度**：
   - 先禁用增强功能（`enabled=false`）部署代码 → 验证读路径。
   - 再开启增强功能，观察 `STATUS` 是否正确刷新。
4. **回滚**：
   - 若需回滚到旧版：
     - 保留 `job_detail_enrichments` 数据不删。
     - 恢复旧版代码时，需额外脚本把 `skills`/`highlights` JSON 拆回旧表。
     - 建议在完全切换前不要清空旧表备份。

## 测试计划

- **单元测试**
  - `JobDetail`：`upsertEnrichment`、`replaceArrayEnrichment`、`contentVersion` 演进。
  - `JobDetailEnrichmentWriter`：幂等写入、版本校验、状态 JSON 构造。
  - `ChatGptJobContentEnrichmentProvider`：解析 JSON → map。
- **集成测试**
  - Spring `@DataJpaTest` 验证 `job_detail_enrichments` 唯一约束、软删除行为。
  - `JobDetailService` 事务流程：模拟 LLM 成功/失败，断言内容与状态。
  - Web 层：`/jobs` & `/jobs/{id}/detail` 返回结构。
- **前端测试**
  - 单元：`JobCardNew` 快照确保不再渲染摘要/亮点。
  - E2E：打开列表页展示标签，详情页展示摘要/亮点占位。
- **回归**：搜索、筛选、分页逻辑保持不变。

## 风险与缓解

| 风险 | 说明 | 缓解 |
| --- | --- | --- |
| 旧数据迁移失败 | JSON 序列化或字符超长导致写入失败 | 迁移脚本分批执行并记录失败 jobId；对 `value_json` 做长度校验 |
| 状态行被误删 | `status` key 被覆写或删除导致重试失效 | `JobDetail` 中 `ensureStatusRow()` 自动重建；删除操作仅设置 `deleted=1` |
| 事务 B 插入冲突 | 并发触发导致唯一约束冲突 | 写入前使用 `SELECT ... FOR UPDATE`，或捕获约束异常并执行 `UPDATE` |
| 前端未适配新字段 | 老版本前端解析失败 | 接口保留 `summary`/`skills`/`highlights` 过渡字段，发布顺序：后端→前端 |

## 测试
⚠️ 未执行（设计评审阶段，仅产出方案，无需运行测试）

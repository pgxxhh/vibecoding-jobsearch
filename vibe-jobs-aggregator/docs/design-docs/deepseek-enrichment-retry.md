# DeepSeek 富化限流与重试实现说明

## 背景

- DeepSeek 富化事件原本由一个 2~4 线程的异步线程池并行消费，命中官方限流后会抛出 HTTP 429 并被直接标记为 `FAILED`。
- 数据表 `job_detail_enrichments` 没有任何重试字段，失败记录会永久停留在失败状态。
- 运行实例无法观察到富化重试情况，也无法限制重试速率。

## 目标

1. **串行消费富化事件**，降低 DeepSeek 请求速率，缓解 429。
2. **自动重试临时失败**，并提供最大尝试次数和指数退避策略。
3. **增强可配置性与可观测性**：所有核心参数通过配置项暴露，状态落库供排查。

## 方案概览

### 1. 单线程异步执行

- 新增 `JobContentEnrichmentExecutorProperties`，默认核心/最大线程数均为 1，队列容量 20，可通过环境变量覆盖。
- `AsyncConfig` 改为按配置初始化 `ThreadPoolTaskExecutor`，保持 `CallerRunsPolicy` 拒绝策略，并在关停时等待队列内任务完成。

### 2. 富化结果落库与重试字段

- `V17__job_detail_enrichment_retries_ddl.sql` 为 `job_detail_enrichments` 表增加以下列：`status_state`、`retry_count`、`next_retry_at`、`last_attempt_at`、`max_attempts`，并建立 `(enrichment_key, status_state, next_retry_at)` 索引。
- `JobDetailEnrichment` 新增上述字段以及 `markSucceeded`、`markRetryScheduled`、`markRetrying`、`markFailedTerminal` 等方法，统一维护状态。
- `JobDetailEnrichmentStatus` 定义 `SUCCESS`/`FAILED`/`RETRY_SCHEDULED`/`RETRYING` 常量，便于代码复用。

### 3. 重试策略与 Writer 调整

- 引入 `JobDetailEnrichmentRetryProperties` 与 `JobDetailEnrichmentRetryStrategy`，支持配置最大重试次数、首个延迟、指数退避倍数、最大延迟、调度周期、批量大小与在途保护时间。
- `JobDetailEnrichmentWriter` 在写入失败时根据 `JobContentEnrichmentResult#isRetryable()` 判断是否重试：
  - 可重试且未超出 `maxAttempts` 时，按策略计算 `nextRetryAt` 并写入 `RETRY_SCHEDULED`，状态 JSON 同步包含 `retryCount`、`nextRetryAt`、`maxAttempts`、`lastAttemptAt`。
  - 不可重试或超过最大次数时，标记 `FAILED` 并清空重试计划。
- 成功写入时重置所有重试字段，状态 JSON 中追加 `retryCount`、`lastAttemptAt` 等诊断信息。

### 4. 定时调度重试

- 新增 `JobDetailEnrichmentRepository`，提供分页查询待重试记录以及将状态切换为 `RETRYING` 的原子更新。
- `JobDetailEnrichmentRetryScheduler` 每隔 `scheduler-interval`（默认 1 分钟）扫描 `RETRY_SCHEDULED` 且到期的记录：
  - 若 `last_attempt_at` 在 `in-flight-guard`（默认 5 分钟）内则跳过，防止重复触发。
  - 成功抢占后发布新的 `JobDetailContentUpdatedEvent`，由单线程执行器异步消费。
- 为保证指纹一致性，新增 `JobContentFingerprintCalculator`，`JobDetailService` 与调度器共享该组件。

### 5. 其他改动

- `JobSnapshot` 新增静态工厂 `from(Job)`，避免重复构造逻辑。
- `JobDetailService` 注入指纹计算器，消除重复代码。
- `JobContentEnrichmentResult` 新增 `isRetryable()`，对 429/5xx 以及客户端异常自动判定为可重试。
- 补充单元测试：
  - `JobContentEnrichmentResultTest` 验证重试判定。
  - `JobDetailEnrichmentRetryStrategyTest` 验证退避计算。
  - `JobDetailEnrichmentWriterTest` 覆盖成功落库与 429 重试路径。

## 配置项一览

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `jobs.detail-enhancement.executor.core-size` | `1` | 富化异步线程数（串行）。 |
| `jobs.detail-enhancement.executor.queue-capacity` | `20` | 等待队列容量。 |
| `jobs.detail-enhancement.retry.enabled` | `true` | 是否启用自动重试。 |
| `jobs.detail-enhancement.retry.max-attempts` | `5` | 最大尝试次数（含首轮调用）。 |
| `jobs.detail-enhancement.retry.initial-delay` | `PT1M` | 首次重试延迟。 |
| `jobs.detail-enhancement.retry.backoff-multiplier` | `2.0` | 指数退避系数。 |
| `jobs.detail-enhancement.retry.max-delay` | `PT30M` | 重试延迟上限。 |
| `jobs.detail-enhancement.retry.scheduler-interval` | `PT1M` | 调度器轮询频率。 |
| `jobs.detail-enhancement.retry.batch-size` | `20` | 单次提取的最大重试任务数。 |
| `jobs.detail-enhancement.retry.in-flight-guard` | `PT5M` | 最近尝试内的保护时间，避免重复调度。 |

## 部署与验证建议

1. 执行 Flyway 迁移（或启动服务自动迁移），确认新列与索引已创建。
2. 校验环境变量/配置文件是否传入新的 executor 与 retry 参数。
3. 部署后观察日志与监控：
   - 确认 `job-enrich-` 线程只有一个活动线程。
   - 触发一次 429，检查状态 JSON 中 `retryCount`、`nextRetryAt` 是否落库。
   - 等待调度器运行，确认记录状态从 `RETRY_SCHEDULED` 转为 `RETRYING`/`SUCCESS`。


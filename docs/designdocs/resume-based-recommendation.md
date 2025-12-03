# 📄 简历上传→岗位推荐端到端设计

## 📌 文档信息
| 字段 | 内容 |
| --- | --- |
| 负责人 | 求职体验工程组 |
| 审核人 | 后端/前端技术负责人 |
| 状态 | 草案 |
| 最近更新 | 2024-05-09 |

## 1. 背景与目标
- 求职者希望通过上传简历快速获得匹配岗位与解释，减少手动筛选成本。
- 现有站点以关键词/标签检索为主，缺少对简历意图的理解与个性化匹配。
- 目标：上线“上传简历→推荐岗位”闭环，提供匹配度、关键技能覆盖、可追溯解释（RAG）和反馈迭代能力。

### 成功指标
| 指标 | 目标 |
| --- | --- |
| 推荐点击率 (CTR@10) | +8% 相比关键词基线 |
| 保存/申请转化率 | +5% |
| RAG 解释引用率 | ≥90% 包含来源标注 |
| 平均生成时延 | ≤3s（不含文件上传） |

## 2. 范围与非目标
- 范围：简历上传、解析、向量化、岗位匹配、RAG 解释、反馈收集与基础监控。
- 非目标：复杂多语言排版 OCR、自动改写简历；后续迭代再评估。

## 3. 总体架构
```mermaid
flowchart LR
  FE[Next.js 前端] --> API[/Resume API/]
  API --> Parser[ResumeParsingService]
  API --> Emb[EmbeddingService]
  API --> Rec[RecommendationService]
  Rec --> VecDB[(向量库 pgvector/OpenSearch)]
  Rec --> MySQL[(MySQL 元数据)]
  Rec --> Rag[RagExplanationService]
  Rag --> LLM[LLM Provider]
  FE <-- API
```

### 设计原则（遵循 `docs/rules.md`）
- 后端遵循 DDD 分层，接口放在 `interfaces`，领域不依赖基础设施。
- 配置化/复用优先，避免硬编码；DDL/DML 迁移放在 `resources/db.migrations`，可重复执行并包含标准审计字段。
- 文档集中在 `docs/`，与前端/后端上下文命名一致。

## 4. 关键流程（Happy Path）
1. 前端上传 PDF/DOC，调用 `POST /api/resumes/upload`，展示解析预览。
2. `ResumeParsingService` 提取文本、拆分板块（个人信息、技能、经历、教育），输出结构化 JSON。
3. `EmbeddingService` 对简历全文与板块生成向量，写入向量库 `resume_embedding` 命名空间。
4. `RecommendationService` 使用简历向量检索岗位向量（复用既有 `job` 索引），混合规则过滤（地点、薪资、经验）。
5. `RagExplanationService` 取简历关键片段与岗位 chunk 构造上下文，调用 LLM 生成“匹配理由”，附带引用。
6. API 返回岗位列表、匹配分、关键技能覆盖率、RAG 理由；前端展示并收集点赞/踩反馈。

## 5. 数据建模与存储
### 5.1 元数据表（MySQL）
- `resume`
  - `id`, `user_id`, `original_filename`, `file_path`, `language`, `parsed_json`, `parse_status`, `create_time`, `update_time`, `deleted default false`
- `resume_feedback`
  - `id`, `resume_id`, `job_id`, `feedback ENUM('LIKE','DISLIKE')`, `comment`, `create_time`, `update_time`, `deleted default false`
- 迁移文件命名示例：`V32_resume_tables_ddl.sql`，可重复执行。

### 5.2 向量索引
- `resume_embedding`
  - `id`, `resume_id`, `section ENUM('FULL','SUMMARY','SKILLS','EXPERIENCE')`, `embedding vector`, `hash`, `version`, `metadata (language, years, top_skills)`
- `job_embedding`（复用现有岗位索引，若无则补全）
  - `job_id`, `embedding vector`, `section ('TITLE','DESC','HIGHLIGHT')`, `version`, `metadata (location, seniority)`
- 索引引擎：pgvector/OpenSearch HNSW，保留 `job_id` 外键便于解释与清理。

## 6. 服务设计（Spring Boot）
### 6.1 接口层（`interfaces.rest`）
- `POST /resumes/upload`
  - Multipart 接收文件；基本校验：类型（PDF/DOC/DOCX）、大小限制、病毒扫描钩子。
  - 返回 `resumeId`、解析状态。
- `GET /resumes/{id}/recommendations?limit=20`
  - 查询匹配岗位，支持过滤：`location`, `remote`, `minSalary`, `experienceYears`。
- `POST /resumes/{id}/feedback`
  - 批量提交 `(jobId, feedback, comment)`，用于重排与评估。

### 6.2 应用/领域层
- `ResumeParsingService`
  - 使用文本抽取器 + 规则/ML 分段；输出 `ResumeProfile`（技能、最近职位、教育、期望地点）。
- `EmbeddingService`
  - 封装向量模型调用（如 bge-m3/OpenAI text-embedding-3-large），支持重试与版本号；对 `ResumeProfile` 的不同段落生成向量。
- `RecommendationService`
  - 混合检索：向量相似度（resume→job）+ 规则过滤 + BM25 兜底。
  - 重排：技能覆盖度、地点匹配度、发布时间衰减；支持阈值兜底到关键词检索。
- `RagExplanationService`
  - 检索：为每个 `jobId` 获取 2-3 个最相关的岗位 chunk 与对应简历片段。
  - Prompt："基于下列简历片段与岗位片段，用中文给出 3 条匹配理由，并用[]引用来源"。
  - 缓存：对 `(resumeId, jobId)` 结果使用缓存/表存储，避免重复生成。
- `FeedbackService`
  - 记录用户反馈，汇总特征（喜欢的技能/地点）调整权重；可暴露事件给个性化训练任务。

### 6.3 基础设施层
- 存储适配：
  - 向量库客户端（pgvector/OpenSearch），实现 `VectorStoreClient`，支持批量 upsert/delete。
  - 对象存储（S3/MinIO）存放原始文件；数据库仅存路径与摘要。
- 任务处理：
  - 文件上传→解析→嵌入生成采用异步事件（`ApplicationEvent` 或消息队列）提升吞吐；失败重试与死信队列。

## 7. 前端方案（Next.js 14）
- 页面：
  - 上传页：文件选择、拖拽、上传进度、解析预览、PII 提示。
  - 推荐列表页：岗位卡片展示匹配度、技能命中、RAG 理由、反馈按钮。
- 状态管理：
  - React Query 持久化 `resumeId` 的推荐列表；失败提示并支持重新拉取。
- API Route：
  - `/api/resumes/upload` 代理 Java 后端；做文件类型/尺寸初步校验。
  - `/api/resumes/[id]/recommendations` 转发查询。
  - `/api/resumes/[id]/feedback` 提交反馈，提供 Zod 校验与错误映射。

## 8. RAG 设计细节
- **文档切分**：岗位与简历均按 512-1024 tokens 分 chunk，保留来源元数据（jobId, section, offset）。
- **检索策略**：简历向量查询岗位索引；同时从简历索引检索与岗位 chunk 的匹配片段，拼接双向证据。
- **消融与兜底**：无向量或低质量简历时回退 BM25；LLM 超时时展示“规则匹配”解释模版。
- **安全**：RAG Prompt 明确禁止虚构技能；对输出运行关键词审查，过滤 PII。

## 9. 质量与评估
- **离线评估**：使用历史点击/申请数据计算 Precision@K、NDCG；对比三种策略：关键词、向量、向量+RAG。
- **在线实验**：A/B 分桶，观察 CTR、申请转化、无结果率、解释点击率。
- **监控**：
  - 向量库 QPS/延迟、索引大小、重建耗时。
  - LLM 生成成功率、耗时、超时/降级次数。
  - 上传/解析失败率，文件大小分布。

## 10. 安全与合规
- 上传文件类型白名单 + 病毒扫描；限制单用户上传频率。
- PII 保护：原始简历可配置过期清理，只持久化必要摘要/向量；访问需鉴权与审计日志。
- 数据隔离：向量库分 `namespace`/`tenant`，防止跨用户泄露。

## 11. 推进计划
1. **M0（1 周）**：上传 + 解析 + 基础向量匹配 MVP（无 RAG），离线评估基线。
2. **M1（1-2 周）**：接入 RAG 解释、缓存与错误兜底；前端展示匹配理由。
3. **M2（2 周）**：反馈收集与重排调优；上线 A/B 实验与监控仪表盘。
4. **M3（持续）**：个性化权重学习、订阅提醒与通知联动。

## 12. 风险与缓解
| 风险 | 影响 | 缓解 |
| --- | --- | --- |
| 简历质量低/格式异常 | 召回差、解释弱 | 添加质量检测与模板化兜底提示；回退关键词匹配 |
| 向量库延迟/成本上升 | 用户体验下降 | 索引分片与缓存；监控+自动降级；定期清理过期简历向量 |
| LLM 幻觉或泄漏 PII | 信任/合规风险 | 严格引用型 Prompt；输出过滤；敏感字段 mask |
| 推荐偏好不符合用户 | 转化降低 | 引入反馈重排与偏好权重；提供筛选器 |

## 13. 预估接口契约（示例）
```yaml
POST /resumes/upload:
  request: multipart/form-data { file }
  response: { resumeId: string, parseStatus: "PENDING"|"READY"|"FAILED" }

GET /resumes/{id}/recommendations:
  query: { limit, location?, remote?, minSalary?, experienceYears? }
  response: [
    {
      jobId: string,
      score: number,
      skillHits: string[],
      explanation: { text: string, citations: [{ source: "resume"|"job", section: string, start: number, end: number }] }
    }
  ]

POST /resumes/{id}/feedback:
  request: { items: [{ jobId: string, feedback: "LIKE"|"DISLIKE", comment?: string }] }
  response: { accepted: number }
```

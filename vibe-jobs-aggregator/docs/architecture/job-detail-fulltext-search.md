# Job Detail Full-Text Search & Plain-Text Persistence

## 背景
现有的职位搜索仅覆盖 `jobs` 主表的基础字段和标签，无法根据职位详情命中结果。同时，详情表 `job_details.content` 以 HTML 形式存储，在原表上直接做 `LIKE` 会导致全表扫描，性能不可控。为此需要：

1. 为详情表引入适合全文检索的字段和索引；
2. 在搜索链路中接入详情关键词匹配，同时兼容现有分页和统计逻辑；
3. 保持数据写入效率，不引入额外不一致；
4. 对外暴露配置，让前端能够显式控制是否扩大到详情检索。

## 方案

### 数据层
- `job_details` 新增 `content_text LONGTEXT` 列，保存 HTML 转纯文本的结果。
- 通过 Flyway `V14__add_job_detail_plain_text_fulltext_ddl.sql` 为新列创建 `FULLTEXT` 索引（使用 MySQL 特性注释保证 H2 环境兼容）。
- `JobDetail` 实体新增 `contentText` 字段，`JobDetailService.saveContent` 在持久化时同步生成纯文本并落库；解析逻辑由 `HtmlTextExtractor` 使用 Jsoup 完成，仍在采集线程同步执行，不改变现有吞吐。

### 搜索链路
- `JobRepository` 引入自定义实现 `JobRepositoryImpl`，在 MySQL 方言下构造 `MATCH ... AGAINST` 查询；当运行在 H2 等无全文支持的环境时回退到 `LIKE`，保证测试可用。
- 查询 SQL 保持原有标签、公司、地点等过滤，并保留 keyset 分页。`countSearch` 与 `searchAfter` 共用拼接逻辑，避免条件漂移。
- `JobController.list` 新增 `searchDetail` 参数，当传入且关键词非空时才激活详情匹配；同时批量拉取命中职位的纯文本详情，计算是否在详情中命中，用于响应中的 `detailMatch` 标记。
- `JobDto` 增加 `detailMatch` 字段，`JobMapper` 支持将该标记写回前端，便于 UI 展示匹配来源。

## 兼容性
- 新增列通过 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`，可重复执行；MySQL 专属索引放置在版本注释中，H2 环境忽略。
- 搜索 SQL 若检测到非 MySQL 方言，则仅走 `LIKE`，保持旧行为；测试依旧运行在 H2。
- API 默认不开启详情搜索，需前端传入 `searchDetail=true` 才启用，与旧接口保持兼容。

## 测试策略
- 扩充 `JobSearchTest` 覆盖基础字段搜索与详情搜索两种分支，验证统计与分页不会回退。
- `HtmlTextExtractor` 通过现有服务调用路径间接覆盖，额外的手工测试确认 HTML 转纯文本的稳定性。

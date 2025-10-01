# Riot Games Career Page 爬虫配置指南

本文说明如何在当前爬虫领域模型下，为 Riot Games 的 Career Page 配置抓取蓝图，并将其挂载到 `job_data_source` 中以便调度。步骤包含解析模板、蓝图、数据源三部分配置，均可在 MySQL 中通过 SQL 完成，也可以在运营后台按相同字段录入。

## 1. 解析模板 (`crawler_parser_template`)

> 解析模板描述如何从列表页节点中抽取标题、链接、地区等字段。Riot 的站点结构需要先确认页面 DOM，以下示例是基于当前线上结构整理，若页面发生变化需重新调整 CSS 选择器。

```sql
INSERT INTO crawler_parser_template (code, description, config_json)
VALUES (
  'riot-default',
  'Riot Games careers list parser',
  JSON_OBJECT(
    'listSelector', 'li.job-card',
    'descriptionField', 'description',
    'tagFields', JSON_ARRAY('teams', 'subteams'),
    'fields', JSON_OBJECT(
      'title',       JSON_OBJECT('type', 'TEXT',       'selector', 'a.job-card__title'),
      'url',         JSON_OBJECT('type', 'ATTRIBUTE',  'selector', 'a.job-card__title', 'attribute', 'href'),
      'externalId',  JSON_OBJECT('type', 'ATTRIBUTE',  'selector', 'a.job-card__title', 'attribute', 'data-job-id'),
      'location',    JSON_OBJECT('type', 'TEXT',       'selector', '.job-card__location'),
      'teams',       JSON_OBJECT('type', 'LIST',       'selector', '.job-card__teams', 'delimiter', ','),
      'subteams',    JSON_OBJECT('type', 'LIST',       'selector', '.job-card__subteams', 'delimiter', ','),
      'description', JSON_OBJECT('type', 'HTML',       'selector', '.job-card__description')
    )
  )
)
ON DUPLICATE KEY UPDATE
  description = VALUES(description),
  config_json = VALUES(config_json);
```

若实际页面的 class 名称不同，请将 `listSelector` 及各字段的 `selector` 调整为真实 DOM 的 CSS 选择器。确保至少保留 `title` 字段，否则蓝图会被判定为未配置完成。

## 2. 蓝图 (`crawler_blueprint`)

蓝图负责分页、限流以及解析模板引用。`config_json` 中的 `entryUrl` 可留空，由 `entry_url` 字段或数据源覆盖。以下示例每页抓取 20 条，使用查询参数翻页。

```sql
INSERT INTO crawler_blueprint (
  code, name, enabled, concurrency_limit, entry_url, parser_template_code, config_json
) VALUES (
  'riot-careers',
  'Riot Games Careers',
  1,
  1,
  'https://www.riotgames.com/en/work-with-us',
  'riot-default',
  JSON_OBJECT(
    'paging', JSON_OBJECT(
      'mode', 'QUERY',
      'parameter', 'page',
      'start', 1,
      'step', 1,
      'sizeParameter', 'page_size'
    ),
    'rateLimit', JSON_OBJECT('requestsPerMinute', 20, 'burst', 2)
  )
)
ON DUPLICATE KEY UPDATE
  enabled = VALUES(enabled),
  entry_url = VALUES(entry_url),
  parser_template_code = VALUES(parser_template_code),
  config_json = VALUES(config_json);
```

如果 Riot Careers 使用动态路由或需要 `offset` 翻页，可将 `mode` 改为 `PATH` / `OFFSET`，并根据 `PagingStrategy` 的约束调整参数。

## 3. 数据源 (`job_data_source` + `job_data_source_company`)

创建新的 `crawler` 类型数据源，并将 Riot Games 作为公司维度挂载。`base_options` 中至少包含 `blueprintCode` 和默认入口 URL，`sourceName` 用于最终写入 `Job.source` 字段。

```sql
-- 主数据源
INSERT INTO job_data_source (code, type, enabled, run_on_startup, require_override, flow, base_options)
VALUES (
  'riot-games-crawler',
  'crawler',
  1,
  0,
  0,
  'LIMITED',
  JSON_OBJECT(
    'blueprintCode', 'riot-careers',
    'entryUrl', 'https://www.riotgames.com/en/work-with-us',
    'sourceName', 'crawler:riot'
  )
)
ON DUPLICATE KEY UPDATE
  enabled = VALUES(enabled),
  base_options = VALUES(base_options),
  flow = VALUES(flow);

-- 公司维度（如需按地区覆盖，可复制多条）
INSERT INTO job_data_source_company (
  data_source_code, reference, display_name, slug, enabled, override_options
) VALUES (
  'riot-games-crawler',
  'riot',
  'Riot Games',
  'riot',
  1,
  JSON_OBJECT(
    'entryUrl', 'https://www.riotgames.com/en/work-with-us?locations=Shanghai'
  )
)
ON DUPLICATE KEY UPDATE
  enabled = VALUES(enabled),
  override_options = VALUES(override_options);
```

如需细分岗位类别，可在 `job_data_source_category` 中新增记录，让调度阶段应用配额和标签匹配。

## 4. 验证调度

1. 启动聚合服务：`mvn spring-boot:run`。
2. 确认 `SourceRegistry` 日志打印 `crawler:riot` 已加载。
3. 手动触发一次抓取（例如通过已有的调度 CLI 或在 `JobIngestionScheduler` 允许启动时自动执行）。
4. 在 `crawler_run_log` 中应看到对应 `blueprint_code = 'riot-careers'` 的记录，并在 `jobs` / `job_detail` 表确认新增岗位数据。

若抓取结果为空，请检查：
- 蓝图 `config_json` 是否正确解析（查看应用日志中的 blueprint 加载日志）。
- 解析模板 CSS 选择器是否能匹配到 DOM 元素。
- 站点是否启用了额外的防爬限制，需要在蓝图 `rateLimit` 或执行引擎层面增加 header/代理。

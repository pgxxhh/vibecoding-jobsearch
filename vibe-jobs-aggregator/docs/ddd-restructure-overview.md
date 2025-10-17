# DDD 目录重构说明

本文档总结了 2024 年 6 月对 `vibe-jobs-aggregator` 后端进行的目录重构与限界上下文调整。目标是在不改变既有业务功能的前提下，引入领域驱动设计（DDD）的目录规范，使团队能够按上下文与分层协作开发。

## 新的顶层结构

```
com/vibe/jobs/
├── bootstrap/                # 应用启动入口与 SpringBoot 配置
├── shared/
│   ├── domain/               # 共享值对象与基类
│   └── infrastructure/
│       ├── config/           # 跨上下文的配置属性、切面、调度配置
│       └── security/         # 安全与加密工具
├── jobposting/
│   ├── domain/               # 招聘职位聚合上下文的实体、值对象
│   ├── application/          # 用例服务、过滤器、富集编排
│   │   ├── dto/
│   │   └── enrichment/
│   ├── infrastructure/
│   │   └── persistence/      # JPA 仓储接口与实现
│   └── interfaces/
│       └── rest/             # REST 控制器、HTTP DTO、限流器
├── ingestion/                # 职位抓取与导入上下文
│   ├── domain/
│   ├── application/
│   └── infrastructure/
│       └── sourceclient/     # 外部 ATS/站点的 SourceClient
├── crawler/                  # 爬虫上下文（原结构保持）
├── datasource/               # 数据源建模（原结构保持）
├── admin/                    # 管理端上下文
└── auth/                     # 鉴权上下文
```

`Bootstrap` 通过 `@SpringBootApplication(scanBasePackages = "com.vibe.jobs")` 覆盖所有上下文，避免因入口包下沉导致的组件扫描缺口。

## 主要迁移对应关系

| 原路径 | 新路径 |
| --- | --- |
| `com.vibe.jobs.domain.*` | `com.vibe.jobs.jobposting.domain.*`（`BaseEntity` 移至 `shared.domain`） |
| `com.vibe.jobs.service.*` | `com.vibe.jobs.jobposting.application.*` |
| `com.vibe.jobs.service.dto` | `com.vibe.jobs.jobposting.application.dto` |
| `com.vibe.jobs.service.enrichment.*` | `com.vibe.jobs.jobposting.application.enrichment.*` |
| `com.vibe.jobs.repo.*` | `com.vibe.jobs.jobposting.infrastructure.persistence.*` |
| `com.vibe.jobs.web.*` | `com.vibe.jobs.jobposting.interfaces.rest.*` |
| `com.vibe.jobs.config.*` | `com.vibe.jobs.shared.infrastructure.config.*` |
| `com.vibe.jobs.security.*` | `com.vibe.jobs.shared.infrastructure.security.*` |
| `com.vibe.jobs.sources.*` | `com.vibe.jobs.ingestion.infrastructure.sourceclient.*` |

测试代码、资源配置、文档引用同步更新，确保包名与新的目录结构一致。

## 配置与集成注意事项

- `AggregatorApplication` 已迁移至 `bootstrap` 包，并显式指定扫描根包；无需额外的 `@ComponentScan` 覆盖。
- `RateLimitingConfig`、`IngestionProperties` 等通用配置集中到 `shared.infrastructure.config`，供多个上下文复用。
- Spring Data JPA 仓储接口新的包路径仍位于 `com.vibe.jobs.jobposting.infrastructure.persistence`，默认扫描范围涵盖该包，无需调整 `@EnableJpaRepositories`。
- `application-test.yml` 的日志配置已同步指向新的富集包名，保持测试输出一致。
- 文档与脚本引用的旧包路径已替换为新结构，避免 onboarding 时的混淆。

## 对测试与构建的影响

- 所有单元测试和集成测试的 `package` 语句、`@WebMvcTest`、`@SpringBootTest` 等注解均指向新包名。
- 推荐通过 `mvn clean verify` 执行全量测试验证回归；在受限网络环境中若 Maven 无法拉取依赖，可使用本地私有仓库或离线缓存后再执行。

## 后续建议

1. 在新的上下文结构下补充 README 中的“贡献指南”，引导新代码进入对应的 `domain/application/infrastructure/interfaces` 分层。
2. 当新增上下文（例如推荐、分析等）时，复用同样的目录模板，保持一致性。
3. 将 `shared.domain.BaseEntity` 逐步替换为更贴合领域的抽象（如聚合根基类），进一步强化限界上下文的自治。

如需了解迁移细节，可参考 Git 历史中的此变更记录。

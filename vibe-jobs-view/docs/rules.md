# Vibe Jobs View 前端开发规范

## 概述
- 目标：与 `vibe-jobs-aggregator` 的 DDD 分层保持对齐，前端按上下文拆分站点体验（求职端、运营端、共享能力），确保用例逻辑、数据访问、样式策略可控可测。
- 原则：**上下文自治**、**显式依赖**、**可复用 UI 基元**、**默认可测试**。
- 适用范围：`vibe-jobs-view/` 下所有代码、文档与 Story/测试资产。

## 目录结构
- 前端目录与聚合器保持“上下文 + 分层”的映射关系：
  - `app/(site)` ↔️ `jobposting` 限界上下文，承载求职者体验；
  - `app/(admin)` ↔️ `admin` 限界上下文，专注运营配置；
  - `app/api/*` ↔️ `interfaces/rest` 层，负责后端适配；
  - `lib/`、`components/` ↔️ `shared` 层，封装跨上下文的共享能力。
- 推荐的目录示例：
  ```
  vibe-jobs-view/
  ├── app/
  │   ├── (site)/
  │   │   ├── layout.tsx         # 聚合根 UI（JobPosting）
  │   │   ├── page.tsx           # 用例：职位检索
  │   │   └── _components/
  │   │       └── filters.tsx    # 子聚合 UI
  │   ├── (admin)/
  │   │   └── admin/
  │   │       ├── ingestion-settings/
  │   │       │   └── page.tsx   # 应用服务：采集策略
  │   │       └── data-sources/
  │   │           └── page.tsx
  │   └── api/
  │       ├── jobs/
  │       │   └── route.ts       # 接口适配器
  │       └── admin/
  │           └── route.ts
  ├── components/
  │   └── job/
  │       ├── JobList.tsx        # 共享 UI 聚合
  │       └── JobDetail.tsx
  ├── lib/
  │   ├── application/
  │   │   └── jobs-query.ts      # 应用层 hooks
  │   ├── domain/
  │   │   └── job.ts             # 领域模型定义
  │   └── infrastructure/
  │       └── http.ts            # Fetch/缓存适配
  └── __tests__/
      └── app-site.test.tsx
  ```
- `_components/`、`_utils/`、`_services/` 仅在 App Router 片段内部使用，跨上下文共享的能力必须上移至 `components`、`lib` 对应分层。

## 依赖约束
- 分层依赖原则：`domain` (纯类型/规则) ← `application` (hooks、状态机) ← `interfaces` (页面/组件)；禁止由内向外的反向引用。
- API Route 作为唯一后端接入层，浏览器端组件不得直接请求 Java 后端域名。
- 依赖图示例：
  ```mermaid
  flowchart TD
    subgraph Shared
      LibDomain[lib/domain/*]
      LibApp[lib/application/*]
      LibInfra[lib/infrastructure/*]
      UIShared[components/*]
    end
    subgraph SiteContext
      SitePages[app/(site)/**/*]
    end
    subgraph AdminContext
      AdminPages[app/(admin)/**/*]
    end
    APIRoutes[app/api/*]

    LibDomain --> LibApp --> LibInfra
    UIShared --> LibApp
    SitePages --> UIShared
    SitePages --> LibApp
    AdminPages --> UIShared
    AdminPages --> LibApp
    APIRoutes --> LibInfra
    LibInfra -.->|fetch| JavaBackend[(vibe-jobs-aggregator REST)]
  ```
- 共享 UI 组件仅依赖 `lib/domain` 暴露的类型，不得嵌入 React Query 逻辑；状态管理放在调用方的应用层 Hook 中。
- 内部 fetch/缓存工具集中于 `lib/infrastructure`，便于在测试中替换或打桩。
- 所有入参需在前端完成基本校验（必填、格式等）并给出友好提示，同时保证后端入口具备同等兜底逻辑，防止未校验数据落入服务。

## 编码规范
- 组件命名使用 `PascalCase`，Hooks 使用 `useCamelCase`；文件名保持 kebab-case 或与组件同名（`ComponentName.tsx`）。
- 新增模块时同步更新 `index.ts` 或集中导出文件，避免跨目录的深层相对路径。
- 受限上下文的数据映射使用显式函数：例如 `mapJobApiToDomain`，避免在组件内部内联 JSON 解析。
- 在应用层 Hook 内处理副作用（React Query、路由跳转）；组件仅负责渲染。
- 所有字符串常量放入 `lib/domain/constants.ts` 或 `app/(context)/_constants.ts`，避免散落魔法值。

### 命名约定
| 类型 | 约定 | 示例 |
| --- | --- | --- |
| 领域类型 | `Noun` + `State/Model` | `Job`, `JobDetail`, `AdminMetricsState` |
| 应用 Hook | `use` + 上下文 + 用例 | `useJobFilters`, `useAdminDataSources` |
| API 适配器 | `<Context>` + `Api` | `jobPostingApi`, `adminConfigApi` |
| API Route 文件 | `route.ts` 下按 HTTP 方法导出 | `app/api/jobs/route.ts` |
| 样式 Token | `vj-` 前缀的 Tailwind 变量 | `vj-border-muted`, `vj-text-accent` |

### 常见反例与纠正
- **反例**：页面组件直接 `fetch('https://backend:8080/api/jobs')`。→ **纠正**：创建 `app/api/jobs/route.ts` 代理后端，并在浏览器使用 `fetch('/api/jobs')`。
- **反例**：在共享组件内部调用 `useQuery` 请求后端。→ **纠正**：在 `lib/application/useJobs.ts` 中封装 React Query，组件通过 props 接收数据或回调。
- **反例**：`app/(site)/utils.ts` 引入 `app/(admin)/data-sources/page.tsx` 中的函数。→ **纠正**：把公共逻辑上移至 `lib/application/data-sources.ts`。

## 数据/接口
- 所有对后端的调用先经过 API Route：
  - 使用 `NextRequest`, `NextResponse` 处理 Cookie 与 Header，保障与后台 `shared.infrastructure.security` 约定一致。
  - 在 `lib/infrastructure/http.ts` 编写 `createBackendClient()`，封装重试、错误映射。
- 与 DDD 对齐的 DTO：
  - `jobposting` → `Job`, `JobsResponse`；
  - `admin` → `AdminIngestionConfig`, `DataSourceDefinition`；
  - `ingestion` 相关只读视图通过 `/api/admin/*/ingestion` 暴露，禁止前端自行拼装。
- 为每个接口提供 Zod 校验或手写类型守卫，位于 `lib/domain/guards.ts`。
- 缓存策略遵循后端聚合根的一致性：查询（`jobposting`）使用 `staleTime` 5 分钟 + `refetchOnWindowFocus=false`；后台突变后务必失效同上下文的 Query Key。

## 样式
- 全量使用 Tailwind Utility + `vibe-jobs-ui-pack` 组件，禁止引入新的 CSS-in-JS 库。
- 设计 Token 统一在 `tailwind.config.js` 的 `theme.extend` 下声明，命名以 `vj-` 前缀。
- 组件样式遵循“语义层（组件）→ 原子类（Tailwind）”的组合，禁止在页面内写大段内联样式。
- 响应式断点与后端上下文保持一致：桌面 ≥ `1024px` 显示双栏，移动端使用抽屉模式；通过 `useMediaQuery` Hook 控制布局。

## 测试
- 最低要求：
  - 应用层 Hook 需具备单元测试，验证状态机及缓存策略；
  - 关键页面（`app/(site)/page.tsx`, `app/(admin)/admin/page.tsx`）应有基础渲染测试或交互快照。
- 使用 Jest + Testing Library，测试文件放在与被测对象同目录或 `__tests__` 下，命名为 `*.test.ts(x)`。
- 对 API Route 使用 `next-test-api-route-handler` 或自定义 fetch mock，验证鉴权头、错误映射。
- CI 需运行 `pnpm lint` + `pnpm test`（后续补充）。提交前确保测试可在无后端依赖情况下运行。

## 文档流程
- 新增上下文或接口后必须同步更新本文件及 `README` 的上下文摘要。
- 设计草案、接口契约建议放在 `docs/design/`，与后端 `docs/design-docs` 呼应，引用相同的聚合名词。
- 发布 PR 时需附上：变更摘要、影响的上下文、`pnpm lint` 结果截图或日志链接。

## 附录
- 参考资料：
  - 《DDD 目录重构说明》：`vibe-jobs-aggregator/docs/ddd-restructure-overview.md`
  - 后端开发规范：`vibe-jobs-aggregator/docs/rules.md`
- 建议工具：
  - `pnpm lint --fix`：快速对齐 ESLint 规则；
  - `pnpm dlx hygen`：可编写脚本生成符合目录约定的页面/组件模板。
- 若需跨上下文复用，请先在 Issue 中评估是否应创建新的共享模块或拆分上下文，避免破坏聚合边界。

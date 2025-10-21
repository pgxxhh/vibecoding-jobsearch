## 概述

编写代码要遵循的一些规则

## 规则

### 🎯 **规则列表**
1.新创建的ddl统一添加到resources db.migrations目录下，迁移文件命名格式为`<V+自然递增序号>_<description>_<ddl or dml>.sql`
新创建的业务表统一要包含以下字段：create_time, update_time, deleted(非null默认false).
所有ddl和dml都要设计成可重复执行

2.不要写补丁逻辑，代码里不能有硬编码，尽量配置化

3.基础原则是复用，清晰简洁

4.后端对外交互层统一放在 `interfaces` 包下（可根据协议再分 `interfaces.rest` 等子包）。涉及目录或包重命名时，必须同步更新所有类的 `package` 声明、相关导入、组件扫描或 `@Import` 配置，并在提交前全局搜索旧包名确认没有残留引用。

5.新的领域代码必须遵循 DDD 分层架构：
   - 领域聚合、值对象不得出现 JPA/Spring 注解或 `EntityManager` 等基础设施依赖；
   - 所有持久化操作通过 `domain.spi` 下的端口接口暴露，命名约定 `<Aggregate>RepositoryPort`；
   - 基础设施层实现端口，类名约定 `<Aggregate>RepositoryAdapter`，内部可以组合 `*JpaRepository` 或其他存储实现；
   - 若需要新增持久化技术，只允许在基础设施层新增适配器，领域与应用层代码不得改动。

6.测试产生的临时文件，测试结束后进行清理

7.不要生成一堆md文件， 如果要更新md, data-source相关统一更新DATA-SOURCE.md.  其他若必要可放在docs目录下

8.对外接口的入参需要做基本校验，前端负责及时提示（如 toast），后端入口必须做兜底校验，确保前端已有的字段校验逻辑在后端同样覆盖。

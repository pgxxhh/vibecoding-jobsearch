## 概述

编写代码要遵循的一些规则

## 规则

### 🎯 **规则列表**
1.新创建的ddl统一添加到resources db.migrations目录下，迁移文件命名格式为`<V+自然递增序号>_<description>_<ddl or dml>.sql`
新创建的业务表统一要包含以下字段：create_time, update_time, deleted(非null默认false).
所有ddl和dml都要设计成可重复执行

2.不要写补丁逻辑，代码里不能有硬编码，尽量配置化

3.基础原则是复用，清晰简洁

4.新的领域参考DDD进行设计

5.测试产生的临时文件，测试结束后进行清理

6.不要生成一堆md文件， 如果要更新md, data-source相关统一更新DATA-SOURCE.md.  其他若必要可放在docs目录下


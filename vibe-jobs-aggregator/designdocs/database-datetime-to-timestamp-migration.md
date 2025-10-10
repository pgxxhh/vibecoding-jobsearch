# 数据库时间字段迁移方案：DATETIME → TIMESTAMP

## 📋 项目概述

### 迁移目标
将所有数据库表中的时间字段从 `DATETIME(6)` 迁移到 `TIMESTAMP(6)`，以获得更好的时区处理和性能优化。

### 业务影响评估
- **影响范围**：所有包含时间字段的表（约47个字段）
- **停机时间**：预计2-4小时（取决于数据量）
- **回滚复杂度**：中等（需要数据转换）
- **风险等级**：高（涉及核心数据结构）

## 🔍 现状分析

### 当前时间字段统计
基于代码分析，主要涉及的表和字段：

#### 核心业务表
```sql
-- jobs 表
posted_at DATETIME(6)
created_at DATETIME(6) NOT NULL
updated_at DATETIME(6) NOT NULL

-- job_details 表  
created_at DATETIME(6) NOT NULL
updated_at DATETIME(6) NOT NULL

-- ingestion_cursors 表
create_time DATETIME(6) NOT NULL
update_time DATETIME(6) NOT NULL
last_posted_at DATETIME(6)
last_ingested_at DATETIME(6) NOT NULL

-- crawler_run_log 表
started_at DATETIME(6)
completed_at DATETIME(6)
```

#### 认证相关表
```sql
-- auth_user 表
created_at DATETIME(6) NOT NULL
updated_at DATETIME(6) NOT NULL
last_login_at DATETIME(6)

-- auth_session 表
expires_at DATETIME(6) NOT NULL
last_sent_at DATETIME(6) NOT NULL
```

### DATETIME vs TIMESTAMP 对比

| 特性 | DATETIME(6) | TIMESTAMP(6) |
|------|-------------|--------------|
| **时区处理** | 无时区信息，存储本地时间 | 支持时区，内部存储UTC |
| **存储范围** | 1000-01-01 到 9999-12-31 | 1970-01-01 到 2038-01-19 |
| **存储空间** | 8 字节 | 4 字节 |
| **默认值** | 需要显式设置 | 可以使用 CURRENT_TIMESTAMP |
| **自动更新** | 需要程序控制 | 支持 ON UPDATE CURRENT_TIMESTAMP |
| **时区转换** | 需要应用层处理 | MySQL 自动处理 |

## ⚠️ 重要限制和风险

### TIMESTAMP 的限制
1. **2038年问题**：TIMESTAMP 只能存储到 2038-01-19 03:14:07 UTC
2. **时间范围限制**：不能存储1970年之前的时间
3. **时区依赖性**：存储会受到 MySQL 时区设置影响

### 风险评估
1. **数据丢失风险**：如果现有数据超出TIMESTAMP范围
2. **应用兼容性**：JPA映射和应用逻辑需要验证
3. **性能影响**：迁移期间的锁表时间
4. **时区问题**：历史数据的时区解释可能不一致

## 🎯 迁移策略

### 策略选择：渐进式迁移
考虑到系统的重要性和复杂性，采用**渐进式迁移**策略：

1. **阶段一**：新表使用TIMESTAMP，旧表保持DATETIME
2. **阶段二**：逐表迁移非核心表
3. **阶段三**：迁移核心业务表
4. **阶段四**：清理和验证

### 技术实现方案

#### 方案A：在线迁移（推荐）
```sql
-- 1. 添加新的TIMESTAMP字段
ALTER TABLE jobs ADD COLUMN created_at_new TIMESTAMP(6) NULL;
ALTER TABLE jobs ADD COLUMN updated_at_new TIMESTAMP(6) NULL;

-- 2. 数据转换和同步
UPDATE jobs SET 
    created_at_new = CONVERT_TZ(created_at, @@session.time_zone, 'UTC'),
    updated_at_new = CONVERT_TZ(updated_at, @@session.time_zone, 'UTC')
WHERE created_at_new IS NULL;

-- 3. 应用层双写期间
-- 应用同时更新新旧字段

-- 4. 验证数据一致性
SELECT COUNT(*) FROM jobs WHERE created_at_new IS NULL;

-- 5. 切换字段名（需要停机）
ALTER TABLE jobs DROP COLUMN created_at;
ALTER TABLE jobs CHANGE COLUMN created_at_new created_at TIMESTAMP(6) NOT NULL;
```

#### 方案B：离线迁移
```sql
-- 直接修改字段类型（需要停机）
ALTER TABLE jobs MODIFY COLUMN created_at TIMESTAMP(6) NOT NULL;
ALTER TABLE jobs MODIFY COLUMN updated_at TIMESTAMP(6) NOT NULL;
```

## 📅 详细实施计划

### 阶段一：准备阶段（1周）
**目标**：环境准备和风险评估

#### 1.1 数据评估（2天）
- [ ] 分析现有数据的时间范围
- [ ] 识别超出TIMESTAMP范围的数据
- [ ] 评估数据量和迁移时间
- [ ] 制定数据备份策略

#### 1.2 环境准备（2天）
- [ ] 准备测试环境
- [ ] 配置数据库时区设置
- [ ] 准备迁移脚本
- [ ] 建立监控和回滚机制

#### 1.3 代码适配（3天）
- [ ] 更新JPA实体类注解
- [ ] 修改Flyway迁移脚本
- [ ] 更新单元测试
- [ ] 性能测试

### 阶段二：非核心表迁移（1周）
**目标**：迁移日志、配置等非核心表

#### 2.1 迁移清单
```sql
-- 优先级：低风险表
crawler_run_log
crawler_cache_entries
admin_change_log
auth_email_challenge
```

#### 2.2 实施步骤
1. **测试环境验证**
2. **生产环境非高峰期迁移**
3. **数据一致性验证**
4. **性能监控**

### 阶段三：核心表迁移（2天，需要维护窗口）
**目标**：迁移核心业务表

#### 3.1 迁移清单
```sql
-- 核心业务表（按依赖关系排序）
jobs
job_details
ingestion_cursors
job_data_sources
```

#### 3.2 实施步骤
1. **维护公告**（提前1周）
2. **完整数据备份**
3. **执行迁移脚本**
4. **验证数据完整性**
5. **恢复服务**

### 阶段四：验证和清理（3天）
**目标**：确保系统稳定运行

#### 4.1 验证项目
- [ ] 数据一致性检查
- [ ] 应用功能测试
- [ ] 性能基准测试
- [ ] 时区显示验证

#### 4.2 清理工作
- [ ] 清理临时字段
- [ ] 更新文档
- [ ] 清理备份数据
- [ ] 性能优化

## 🔧 技术实现细节

### 数据验证脚本
```sql
-- 检查数据范围
SELECT 
    'jobs' as table_name,
    MIN(created_at) as min_time,
    MAX(created_at) as max_time,
    COUNT(*) as total_rows
FROM jobs
WHERE created_at < '1970-01-01' OR created_at > '2038-01-19';

-- 验证时区转换
SELECT 
    created_at as original,
    CONVERT_TZ(created_at, @@session.time_zone, 'UTC') as utc_converted,
    FROM_UNIXTIME(UNIX_TIMESTAMP(created_at)) as timestamp_converted
FROM jobs 
LIMIT 10;
```

### JPA 实体类更新
```java
@Entity
@Table(name = "jobs")
public class JobEntity {
    
    // 之前：DATETIME映射
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    // 之后：TIMESTAMP映射（无需代码更改，但需要验证）
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @PrePersist
    public void onInsert() {
        if (createdAt == null) {
            createdAt = Instant.now(); // 确保UTC时间
        }
    }
}
```

### Flyway 迁移脚本
```sql
-- V15__migrate_datetime_to_timestamp_phase1.sql
-- 非核心表迁移

-- crawler_run_log 表
ALTER TABLE crawler_run_log 
    ADD COLUMN started_at_new TIMESTAMP(6) NULL,
    ADD COLUMN completed_at_new TIMESTAMP(6) NULL;

UPDATE crawler_run_log SET 
    started_at_new = CASE 
        WHEN started_at IS NOT NULL THEN 
            CONVERT_TZ(started_at, @@session.time_zone, 'UTC')
        ELSE NULL 
    END,
    completed_at_new = CASE 
        WHEN completed_at IS NOT NULL THEN 
            CONVERT_TZ(completed_at, @@session.time_zone, 'UTC')
        ELSE NULL 
    END;

-- 验证数据
SELECT COUNT(*) FROM crawler_run_log 
WHERE (started_at IS NOT NULL AND started_at_new IS NULL)
   OR (completed_at IS NOT NULL AND completed_at_new IS NULL);

-- 如果验证通过，切换字段
ALTER TABLE crawler_run_log 
    DROP COLUMN started_at,
    DROP COLUMN completed_at,
    CHANGE COLUMN started_at_new started_at TIMESTAMP(6) NULL,
    CHANGE COLUMN completed_at_new completed_at TIMESTAMP(6) NULL;
```

## 🚀 上线计划

### 上线时间窗口
- **测试环境**：工作日任意时间
- **生产环境阶段二**：周末凌晨 2:00-4:00
- **生产环境阶段三**：周六维护窗口 2:00-6:00

### 上线检查清单

#### 上线前（T-24h）
- [ ] 完成测试环境验证
- [ ] 准备生产环境备份
- [ ] 通知相关团队维护窗口
- [ ] 准备回滚方案
- [ ] 确认监控和告警

#### 上线中（T0）
- [ ] 数据库完整备份
- [ ] 执行迁移脚本
- [ ] 实时监控迁移进度
- [ ] 验证数据完整性
- [ ] 应用功能验证

#### 上线后（T+2h）
- [ ] 系统功能全面测试
- [ ] 性能指标监控
- [ ] 时区显示验证
- [ ] 清理临时数据
- [ ] 更新运维文档

### 回滚方案
```sql
-- 紧急回滚脚本（如果新字段还存在）
ALTER TABLE jobs 
    DROP COLUMN created_at,
    CHANGE COLUMN created_at_old created_at DATETIME(6) NOT NULL;

-- 数据恢复（从备份）
RESTORE TABLE jobs FROM backup_20241010_jobs;
```

## 📊 监控和验证

### 关键监控指标
1. **数据一致性**：新旧字段数据对比
2. **应用性能**：响应时间和错误率
3. **数据库性能**：查询执行时间
4. **时区显示**：前端时间显示正确性

### 验证SQL脚本
```sql
-- 数据完整性检查
SELECT 
    table_name,
    column_name,
    data_type,
    is_nullable
FROM information_schema.columns 
WHERE table_schema = 'vibejobs' 
AND data_type IN ('timestamp', 'datetime');

-- 时区转换验证
SELECT 
    id,
    created_at,
    UNIX_TIMESTAMP(created_at) as timestamp_value,
    FROM_UNIXTIME(UNIX_TIMESTAMP(created_at)) as converted_back
FROM jobs 
LIMIT 10;
```

## 🎯 成功标准

### 技术指标
- [ ] 所有时间字段成功转换为TIMESTAMP
- [ ] 数据零丢失，时间值保持一致
- [ ] 应用性能无明显下降（<5%）
- [ ] 前端时间显示正确（东八区）

### 业务指标
- [ ] 系统功能完全正常
- [ ] 用户体验无影响
- [ ] 数据查询性能提升
- [ ] 运维复杂度降低

## ⚡ 风险控制

### 主要风险
1. **数据范围超限**：历史数据可能超出TIMESTAMP范围
2. **时区混乱**：不同环境时区设置不一致
3. **应用兼容性**：JPA映射或查询逻辑问题
4. **迁移时间过长**：影响业务连续性

### 风险缓解措施
1. **充分测试**：在测试环境完全模拟生产场景
2. **分阶段实施**：降低单次变更影响范围
3. **实时监控**：迁移过程全程监控
4. **快速回滚**：准备完整的回滚方案
5. **数据备份**：多重备份确保数据安全

## 📚 相关文档

- [MySQL TIMESTAMP vs DATETIME 官方文档](https://dev.mysql.com/doc/refman/8.0/en/datetime.html)
- [时区处理最佳实践](./timezone-handling-solution-v2.md)
- [Flyway 迁移指南](https://flywaydb.org/documentation/)
- [JPA 时间类型映射](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.entity-persistence.saving-entities.creation-update-times)

## 🏁 总结

这个迁移方案采用**渐进式、低风险**的策略，通过充分的测试和验证，确保数据库时间字段的平滑迁移。虽然TIMESTAMP有一些限制，但对于大多数业务场景，其时区处理优势和存储效率优化是值得的。

**关键成功因素**：
1. 充分的测试和验证
2. 详细的实施计划和时间控制  
3. 完善的监控和回滚机制
4. 团队间的良好沟通协调

执行此方案需要数据库管理员、后端开发和运维团队的密切配合，建议在实施前进行内部评审和风险评估。
# 时区处理实施指南 v2.0 - 前端处理方案

## 方案核心

```
数据库(UTC) → API(标准UTC格式) → 前端(转换为东八区显示)
```

**关键决策：API返回标准UTC时间，前端负责时区转换和本地化显示**

## 为什么这样设计？

1. **API标准化**：返回ISO 8601标准格式，符合RESTful最佳实践
2. **职责分离**：时区显示是UI关注点，应该在前端处理
3. **扩展性好**：未来支持多时区用户，只需前端配置
4. **缓存友好**：API响应格式固定，有利于缓存

## 实施步骤

### Step 1: 后端配置（最小化改动）

#### 1.1 确保时区配置正确
```java
// src/main/java/com/vibe/jobs/config/TimeZoneConfig.java
@Configuration
public class TimeZoneConfig {
    
    @PostConstruct
    public void setDefaultTimeZone() {
        // 确保JVM默认时区为UTC
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
    
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // 禁用时间戳格式，使用ISO 8601字符串格式
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
```

#### 1.2 验证API响应格式
现有的Entity已经使用`Instant`类型，Jackson会自动序列化为标准格式：
```java
// IngestionCursorEntity.java - 无需修改
@Column(name = "create_time", nullable = false, updatable = false)
private Instant createTime; // Jackson自动序列化为 "2024-01-01T10:00:00Z"
```

#### 1.3 API响应示例
```json
{
  "id": 1,
  "sourceCode": "EXAMPLE", 
  "createTime": "2024-01-01T10:00:00Z",    // UTC时间
  "updateTime": "2024-01-01T12:30:00Z",    // UTC时间
  "lastIngestedAt": "2024-01-01T12:30:00Z" // UTC时间
}
```

### Step 2: 前端时区处理（核心工作）

#### 2.1 安装依赖
```bash
cd vibe-jobs-view
pnpm add date-fns date-fns-tz
```

#### 2.2 创建时区配置
```typescript
// lib/timezone-config.ts
export const TIMEZONE_CONFIG = {
  // 目标时区
  TARGET_TIMEZONE: 'Asia/Shanghai',
  // 显示格式配置
  FORMATS: {
    DATETIME: 'yyyy-MM-dd HH:mm:ss',
    DATE: 'yyyy-MM-dd',
    TIME: 'HH:mm:ss',
    SHORT_DATETIME: 'MM-dd HH:mm'
  }
} as const;
```

#### 2.3 创建时间工具类
```typescript
// lib/time-utils.ts
import { format, parseISO, formatInTimeZone } from 'date-fns-tz';
import { formatDistanceToNow } from 'date-fns';
import { zhCN } from 'date-fns/locale';
import { TIMEZONE_CONFIG } from './timezone-config';

export class TimeUtils {
  
  /**
   * 将UTC时间字符串转换为东八区显示
   */
  static toLocal(utcString: string | null, formatType: keyof typeof TIMEZONE_CONFIG.FORMATS | 'relative' = 'DATETIME'): string {
    if (!utcString) return '--';
    
    try {
      const utcDate = parseISO(utcString);
      
      if (formatType === 'relative') {
        return formatDistanceToNow(utcDate, { 
          addSuffix: true, 
          locale: zhCN 
        });
      }
      
      const pattern = TIMEZONE_CONFIG.FORMATS[formatType] || TIMEZONE_CONFIG.FORMATS.DATETIME;
      return formatInTimeZone(utcDate, TIMEZONE_CONFIG.TARGET_TIMEZONE, pattern);
      
    } catch (error) {
      console.error('Failed to format time:', utcString, error);
      return '--';
    }
  }
  
  /**
   * 将用户输入的本地时间转换为UTC（用于API请求）
   */
  static toUTC(localDateTimeString: string): string {
    try {
      // 假设用户输入是东八区时间
      const localDate = new Date(localDateTimeString + ' GMT+0800');
      return localDate.toISOString();
    } catch (error) {
      console.error('Failed to convert to UTC:', localDateTimeString, error);
      throw new Error('Invalid date format');
    }
  }
  
  /**
   * 获取当前UTC时间字符串（用于API请求）
   */
  static nowUTC(): string {
    return new Date().toISOString();
  }
}
```

#### 2.4 创建时间显示组件
```typescript
// components/TimeDisplay.tsx
import { useMemo } from 'react';
import { TimeUtils } from '@/lib/time-utils';

interface TimeDisplayProps {
  /** UTC时间字符串 */
  utcTime: string | null | undefined;
  /** 显示格式 */
  format?: 'DATETIME' | 'DATE' | 'TIME' | 'SHORT_DATETIME' | 'relative';
  /** CSS类名 */
  className?: string;
  /** 是否显示tooltip */
  showTooltip?: boolean;
}

export function TimeDisplay({ 
  utcTime, 
  format = 'DATETIME', 
  className = '',
  showTooltip = true 
}: TimeDisplayProps) {
  
  const displayTime = useMemo(() => {
    return TimeUtils.toLocal(utcTime, format);
  }, [utcTime, format]);
  
  const tooltipText = useMemo(() => {
    if (!showTooltip || !utcTime) return undefined;
    
    return [
      `本地时间: ${TimeUtils.toLocal(utcTime, 'DATETIME')}`,
      `UTC时间: ${utcTime}`,
      `相对时间: ${TimeUtils.toLocal(utcTime, 'relative')}`
    ].join('\n');
  }, [utcTime, showTooltip]);
  
  return (
    <span 
      className={className}
      title={tooltipText}
    >
      {displayTime}
    </span>
  );
}

// 便捷的特化组件
export function RelativeTime({ utcTime, className }: { utcTime: string | null; className?: string }) {
  return <TimeDisplay utcTime={utcTime} format="relative" className={className} />;
}

export function DateOnly({ utcTime, className }: { utcTime: string | null; className?: string }) {
  return <TimeDisplay utcTime={utcTime} format="DATE" className={className} />;
}
```

### Step 3: 更新现有组件

#### 3.1 更新页面组件示例
```typescript
// 原来的代码
function JobCard({ job }) {
  return (
    <div>
      <h3>{job.title}</h3>
      <p>创建时间: {job.createTime}</p> {/* 显示UTC字符串，不友好 */}
    </div>
  );
}

// 更新后的代码
import { TimeDisplay, RelativeTime } from '@/components/TimeDisplay';

function JobCard({ job }) {
  return (
    <div>
      <h3>{job.title}</h3>
      <div className="flex gap-4 text-sm text-gray-600">
        <RelativeTime utcTime={job.createTime} />
        <TimeDisplay utcTime={job.updateTime} format="SHORT_DATETIME" />
      </div>
    </div>
  );
}
```

#### 3.2 表格中的时间显示
```typescript
function DataTable({ items }) {
  return (
    <table>
      <thead>
        <tr>
          <th>名称</th>
          <th>创建时间</th>
          <th>最后更新</th>
        </tr>
      </thead>
      <tbody>
        {items.map(item => (
          <tr key={item.id}>
            <td>{item.name}</td>
            <td>
              <TimeDisplay utcTime={item.createTime} format="DATETIME" />
            </td>
            <td>
              <RelativeTime utcTime={item.updateTime} />
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
```

## 需要修改的文件

### 后端（最小改动）
1. **新增**: `src/main/java/com/vibe/jobs/config/TimeZoneConfig.java`
2. **验证**: 确认现有API返回的时间格式正确

### 前端（主要工作）
1. **新增**:
   - `lib/timezone-config.ts`
   - `lib/time-utils.ts`  
   - `components/TimeDisplay.tsx`

2. **更新**: 所有显示时间的组件文件
   - `app/(site)/page.tsx`
   - 其他包含时间显示的页面和组件

## 验证测试

### 1. 后端API测试
```bash
# 测试API返回的时间格式
curl http://localhost:8080/api/xxx | jq '.createTime'
# 期望输出: "2024-01-01T10:00:00Z"
```

### 2. 前端单元测试
```typescript
// __tests__/time-utils.test.ts
import { TimeUtils } from '@/lib/time-utils';

describe('TimeUtils', () => {
  test('should convert UTC to Asia/Shanghai', () => {
    const utc = "2024-01-01T10:00:00Z";
    const local = TimeUtils.toLocal(utc, 'DATETIME');
    expect(local).toBe("2024-01-01 18:00:00");
  });
  
  test('should handle null values', () => {
    expect(TimeUtils.toLocal(null)).toBe('--');
  });
});
```

### 3. 端到端验证
- [ ] 页面显示的时间是东八区时间
- [ ] tooltip显示正确的时间信息  
- [ ] 相对时间显示正确（如"2小时前"）
- [ ] API返回标准UTC格式
- [ ] 不同时间格式显示正确

## 优势总结

✅ **API标准化**: 符合REST API最佳实践  
✅ **职责清晰**: 前端负责显示，后端负责数据  
✅ **扩展性强**: 未来支持多时区只需前端配置  
✅ **缓存友好**: API响应格式固定  
✅ **国际化准备**: 同一API可服务不同时区用户

这个方案避免了重复处理时区的问题，让每一层都有明确的职责。你觉得这样的设计如何？
# Location Filter Configuration Guide

## 概述

新增了可配置的location过滤功能，可以根据地理位置过滤职位数据，支持只获取特定国家、地区或城市的职位信息。

## 功能特性

- ✅ **可配置的地理过滤**: 支持按国家、城市、关键词过滤
- ✅ **包含/排除模式**: 既可以指定包含的位置，也可以排除不需要的位置  
- ✅ **实时统计**: 显示过滤前后的职位数量变化
- ✅ **灵活配置**: 支持环境变量和配置文件两种方式
- ✅ **向下兼容**: 默认不启用过滤，保持原有行为

## 配置方式

### 1. 基本配置结构

```yaml
ingestion:
  locationFilter:
    enabled: true/false              # 是否启用location过滤
    includeCountries: []             # 包含的国家列表
    includeRegions: []               # 包含的地区列表
    includeCities: []                # 包含的城市列表
    includeKeywords: []              # 包含的关键词列表
    excludeCountries: []             # 排除的国家列表
    excludeRegions: []               # 排除的地区列表
    excludeCities: []                # 排除的城市列表
    excludeKeywords: []              # 排除的关键词列表
```

### 2. 环境变量配置

```bash
# 启用/禁用location过滤
export LOCATION_FILTER_ENABLED=true
```

### 3. 配置示例

#### 示例1: 只获取中国地区职位

```yaml
ingestion:
  locationFilter:
    enabled: true
    includeCountries:
      - "china"
    includeCities:
      - "beijing"
      - "shanghai"
      - "shenzhen"
      - "guangzhou"
      - "hangzhou"
    includeKeywords:
      - "china"
      - "remote - china"
```

#### 示例2: 获取亚太地区职位

```yaml
ingestion:
  locationFilter:
    enabled: true
    includeCountries:
      - "china"
      - "singapore"
      - "hong kong"
      - "taiwan"
      - "japan"
    includeCities:
      - "beijing"
      - "shanghai"
      - "singapore"
      - "hong kong"
      - "tokyo"
    includeKeywords:
      - "remote - asia"
      - "asia pacific"
      - "apac"
```

#### 示例3: 排除欧美地区

```yaml
ingestion:
  locationFilter:
    enabled: true
    excludeCountries:
      - "united states"
      - "usa"
      - "canada"
    excludeRegions:
      - "europe"
    excludeKeywords:
      - "remote - us"
      - "remote - europe"
```

## 过滤逻辑

### 匹配规则

1. **大小写不敏感**: 所有匹配都是大小写不敏感的
2. **子串匹配**: 使用contains()匹配，支持部分匹配
3. **排除优先**: 如果location匹配任何排除条件，直接拒绝
4. **包含条件**: 必须至少匹配一个包含条件才接受

### 处理流程

```
1. 检查是否启用过滤 -> 未启用则返回所有职位
2. 检查排除条件 -> 匹配则拒绝
3. 检查包含条件 -> 无包含条件或匹配至少一个则接受
4. 记录过滤统计信息
```

## 使用方式

### 启动配置

在`application.yml`中配置location过滤，应用启动时会显示过滤器状态：

```
[CareersApiStartupRunner] Location filter: ENABLED
  Include countries: [china, singapore, hong kong]
  Include cities: [beijing, shanghai, shenzhen]
```

### 动态调整

可以通过以下方式调整配置：

1. **修改配置文件**: 更新`application.yml`后重启应用
2. **环境变量**: 设置`LOCATION_FILTER_ENABLED=true/false`
3. **Profile配置**: 为不同环境创建不同的配置文件

### 监控效果

应用会在日志中显示过滤效果：

```
INFO LocationFilterService - Location filter: 100 jobs -> 23 jobs (filtered out 77)
```

## 当前数据分布

根据现有数据库统计，职位location分布如下：

| Location | Count |
|----------|-------|
| New York | 292 |
| Singapore | 222 |
| London | 183 |
| Hong Kong | 167 |
| Shanghai | 29 |
| Beijing | 11 |

启用中国地区过滤后，可以显著提高中国相关职位的比例。

## 最佳实践

1. **逐步启用**: 建议先在测试环境验证配置效果
2. **关键词优化**: 根据实际数据调整include/exclude关键词
3. **监控效果**: 关注过滤后的职位数量和质量
4. **定期更新**: 根据新增的location数据调整配置

## 故障排除

### 常见问题

1. **过滤太严格**: 检查exclude条件是否过于宽泛
2. **没有过滤效果**: 确认`enabled: true`且配置了正确的条件
3. **意外过滤**: 检查关键词是否存在冲突

### 调试方法

1. **查看日志**: 观察过滤前后的数量变化
2. **测试配置**: 使用单元测试验证过滤逻辑
3. **分步配置**: 先配置宽松条件，再逐步细化
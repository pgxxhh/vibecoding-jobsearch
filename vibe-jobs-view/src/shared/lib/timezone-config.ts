/**
 * 时区配置
 * 
 * 统一管理系统的时区设置和显示格式
 */
export const TIMEZONE_CONFIG = {
  // 目标时区（可以后续改为用户配置）
  TARGET_TIMEZONE: 'Asia/Shanghai',
  
  // 时区偏移
  TARGET_OFFSET: '+08:00',
  
  // 显示格式配置
  FORMATS: {
    DATETIME: 'yyyy-MM-dd HH:mm:ss',
    DATE: 'yyyy-MM-dd',
    TIME: 'HH:mm:ss',
    SHORT_DATETIME: 'MM-dd HH:mm',
    MONTH_DAY: 'MM-dd'
  }
} as const;

// 导出类型以便TypeScript使用
export type FormatType = keyof typeof TIMEZONE_CONFIG.FORMATS;
import { format, parseISO, formatDistanceToNow } from 'date-fns';
import { formatInTimeZone } from 'date-fns-tz';
import { zhCN } from 'date-fns/locale';
import { TIMEZONE_CONFIG, type FormatType } from './timezone-config';

/**
 * 时间处理工具类
 * 
 * 负责UTC时间与东八区时间之间的转换
 * 以及各种时间格式的处理
 */
export class TimeUtils {
  
  /**
   * 将UTC时间字符串转换为东八区显示
   * @param utcString UTC时间字符串，如 "2024-01-01T10:00:00Z"
   * @param formatType 显示格式类型
   * @returns 格式化后的本地时间字符串
   */
  static toLocal(
    utcString: string | null | undefined, 
    formatType: FormatType | 'relative' = 'DATETIME'
  ): string {
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
      console.error('Failed to format UTC time:', utcString, error);
      return '--';
    }
  }
  
  /**
   * 将用户输入的本地日期时间转换为UTC（用于API查询参数）
   * @param localDateTime 用户输入的本地时间，如 "2024-01-01" 或 "2024-01-01 10:30:00"
   * @param isEndOfDay 是否为结束时间（自动设置为当天23:59:59）
   * @returns UTC时间的ISO字符串
   */
  static toSearchUTC(localDateTime: string, isEndOfDay = false): string {
    try {
      let dateTimeStr = localDateTime.trim();
      
      // 如果只有日期，补充时间部分
      if (dateTimeStr.match(/^\d{4}-\d{2}-\d{2}$/)) {
        dateTimeStr += isEndOfDay ? ' 23:59:59' : ' 00:00:00';
      }
      
      // 构造东八区时间并转换为UTC
      // 注意：这里假设用户输入的是东八区时间
      const localDate = new Date(dateTimeStr + ' GMT+0800');
      if (isNaN(localDate.getTime())) {
        throw new Error('Invalid date');
      }
      
      return localDate.toISOString();
      
    } catch (error) {
      console.error('Failed to convert search time to UTC:', localDateTime, error);
      throw new Error(`Invalid date format: ${localDateTime}`);
    }
  }
  
  /**
   * 为搜索创建时间范围参数
   * @param startDate 开始日期，如 "2024-01-01"
   * @param endDate 结束日期，如 "2024-01-02"  
   * @returns 包含UTC格式startTime和endTime的对象
   */
  static createSearchTimeRange(startDate: string, endDate: string) {
    return {
      startTime: this.toSearchUTC(startDate, false),  // 开始时间：00:00:00
      endTime: this.toSearchUTC(endDate, true)        // 结束时间：23:59:59
    };
  }
  
  /**
   * 将UTC时间转换为用户友好的本地时间描述
   * @param startUtc UTC开始时间
   * @param endUtc UTC结束时间
   * @returns 友好的时间范围描述
   */
  static describeSearchRange(startUtc: string, endUtc: string): string {
    const start = this.toLocal(startUtc, 'DATE');
    const end = this.toLocal(endUtc, 'DATE');
    return start === end ? `${start}当天` : `${start} 至 ${end}`;
  }
  
  /**
   * 获取当前UTC时间字符串（用于API请求）
   * @returns 当前UTC时间的ISO字符串
   */
  static nowUTC(): string {
    return new Date().toISOString();
  }
  
  /**
   * 获取当前本地时间字符串（用于默认值显示）
   * @param formatType 格式类型
   * @returns 当前本地时间的格式化字符串
   */
  static nowLocal(formatType: FormatType = 'DATETIME'): string {
    return this.toLocal(this.nowUTC(), formatType);
  }
  
  /**
   * 验证时间字符串格式是否正确
   * @param timeString 时间字符串
   * @returns 是否为有效的时间格式
   */
  static isValidTimeString(timeString: string): boolean {
    try {
      const date = parseISO(timeString);
      return !isNaN(date.getTime());
    } catch {
      return false;
    }
  }
}
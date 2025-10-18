import { useMemo } from 'react';
import { TimeUtils } from '@/shared/lib/time-utils';
import { type FormatType } from '@/shared/lib/timezone-config';

interface TimeDisplayProps {
  /** UTC时间字符串 */
  utcTime: string | null | undefined;
  /** 显示格式 */
  format?: FormatType | 'relative';
  /** CSS类名 */
  className?: string;
  /** 是否显示tooltip */
  showTooltip?: boolean;
  /** 当时间为空时显示的文本 */
  placeholder?: string;
}

/**
 * 通用时间显示组件
 * 
 * 自动将UTC时间转换为东八区时间显示
 * 支持多种格式和相对时间显示
 */
export function TimeDisplay({ 
  utcTime, 
  format = 'DATETIME', 
  className = '',
  showTooltip = true,
  placeholder = '--'
}: TimeDisplayProps) {
  
  const displayTime = useMemo(() => {
    if (!utcTime) return placeholder;
    return TimeUtils.toLocal(utcTime, format);
  }, [utcTime, format, placeholder]);
  
  const tooltipText = useMemo(() => {
    if (!showTooltip || !utcTime) return undefined;
    
    const parts = [
      `本地时间: ${TimeUtils.toLocal(utcTime, 'DATETIME')}`,
      `UTC时间: ${utcTime}`
    ];
    
    // 如果不是相对时间格式，额外显示相对时间
    if (format !== 'relative') {
      parts.push(`相对时间: ${TimeUtils.toLocal(utcTime, 'relative')}`);
    }
    
    return parts.join('\n');
  }, [utcTime, format, showTooltip]);
  
  return (
    <span 
      className={className}
      title={tooltipText}
    >
      {displayTime}
    </span>
  );
}

/**
 * 相对时间显示组件（如：2小时前）
 */
export function RelativeTime({ 
  utcTime, 
  className,
  placeholder = '--' 
}: { 
  utcTime: string | null | undefined;
  className?: string;
  placeholder?: string;
}) {
  return (
    <TimeDisplay 
      utcTime={utcTime} 
      format="relative" 
      className={className}
      placeholder={placeholder}
    />
  );
}

/**
 * 仅显示日期的组件
 */
export function DateOnly({ 
  utcTime, 
  className,
  placeholder = '--'
}: { 
  utcTime: string | null | undefined;
  className?: string;
  placeholder?: string;
}) {
  return (
    <TimeDisplay 
      utcTime={utcTime} 
      format="DATE" 
      className={className}
      placeholder={placeholder}
    />
  );
}

/**
 * 紧凑的日期时间显示组件（如：01-15 14:30）
 */
export function CompactDateTime({ 
  utcTime, 
  className,
  placeholder = '--'
}: { 
  utcTime: string | null | undefined;
  className?: string;
  placeholder?: string;
}) {
  return (
    <TimeDisplay 
      utcTime={utcTime} 
      format="SHORT_DATETIME" 
      className={className}
      placeholder={placeholder}
    />
  );
}
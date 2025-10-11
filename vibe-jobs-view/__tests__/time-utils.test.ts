import { TimeUtils } from '@/lib/time-utils';

describe('TimeUtils', () => {
  describe('toLocal', () => {
    it('should convert UTC to Asia/Shanghai timezone', () => {
      const utc = "2024-01-01T10:00:00Z";
      const local = TimeUtils.toLocal(utc, 'DATETIME');
      expect(local).toBe("2024-01-01 18:00:00");
    });

    it('should handle different format types', () => {
      const utc = "2024-01-01T10:00:00Z";
      
      expect(TimeUtils.toLocal(utc, 'DATE')).toBe("2024-01-01");
      expect(TimeUtils.toLocal(utc, 'TIME')).toBe("18:00:00");
      expect(TimeUtils.toLocal(utc, 'SHORT_DATETIME')).toBe("01-01 18:00");
    });

    it('should handle null and undefined values', () => {
      expect(TimeUtils.toLocal(null)).toBe('--');
      expect(TimeUtils.toLocal(undefined)).toBe('--');
      expect(TimeUtils.toLocal('')).toBe('--');
    });

    it('should handle invalid time strings gracefully', () => {
      expect(TimeUtils.toLocal('invalid-time')).toBe('--');
    });
  });

  describe('toSearchUTC', () => {
    it('should convert date-only to start of day UTC', () => {
      const result = TimeUtils.toSearchUTC('2024-01-01', false);
      // 2024-01-01 00:00:00 CST = 2023-12-31 16:00:00 UTC
      expect(result).toMatch(/2023-12-31T16:00:00/);
    });

    it('should convert date-only to end of day UTC', () => {
      const result = TimeUtils.toSearchUTC('2024-01-01', true);
      // 2024-01-01 23:59:59 CST = 2024-01-01 15:59:59 UTC
      expect(result).toMatch(/2024-01-01T15:59:59/);
    });

    it('should handle datetime strings', () => {
      const result = TimeUtils.toSearchUTC('2024-01-01 12:00:00', false);
      // 2024-01-01 12:00:00 CST = 2024-01-01 04:00:00 UTC
      expect(result).toMatch(/2024-01-01T04:00:00/);
    });

    it('should throw error for invalid dates', () => {
      expect(() => TimeUtils.toSearchUTC('2024-13-01')).toThrow();
    });
  });

  describe('createSearchTimeRange', () => {
    it('should create correct time range', () => {
      const range = TimeUtils.createSearchTimeRange('2024-01-01', '2024-01-02');
      
      // 验证范围的基本格式
      expect(range.startTime).toMatch(/2023-12-31T16:00:00/); // 2024-01-01 00:00:00 CST
      expect(range.endTime).toMatch(/2024-01-02T15:59:59/);   // 2024-01-02 23:59:59 CST
    });
  });

  describe('nowUTC', () => {
    it('should return current UTC time as ISO string', () => {
      const result = TimeUtils.nowUTC();
      expect(result).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d{3}Z$/);
    });
  });

  describe('isValidTimeString', () => {
    it('should validate correct ISO time strings', () => {
      expect(TimeUtils.isValidTimeString('2024-01-01T10:00:00Z')).toBe(true);
      expect(TimeUtils.isValidTimeString('2024-01-01T10:00:00.000Z')).toBe(true);
    });

    it('should reject invalid time strings', () => {
      expect(TimeUtils.isValidTimeString('invalid-time')).toBe(false);
      expect(TimeUtils.isValidTimeString('')).toBe(false);
    });
  });
});
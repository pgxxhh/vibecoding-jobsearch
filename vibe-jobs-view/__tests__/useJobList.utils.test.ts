import {
  computeDateCutoff,
  filterJobsByDate,
  encodeCursorValue,
  DATE_FILTER_SIZE_MULTIPLIER,
} from '@/modules/job-search/hooks/useJobList';
import type { Job } from '@/modules/job-search/types';

describe('useJobList helpers', () => {
  describe('computeDateCutoff', () => {
    const realNow = Date.now;

    beforeAll(() => {
      Date.now = () => new Date('2024-08-01T00:00:00Z').getTime();
    });

    afterAll(() => {
      Date.now = realNow;
    });

    it('returns null when value is empty or invalid', () => {
      expect(computeDateCutoff(undefined)).toBeNull();
      expect(computeDateCutoff('')).toBeNull();
      expect(computeDateCutoff('abc')).toBeNull();
      expect(computeDateCutoff('-1')).toBeNull();
    });

    it('returns timestamp when value is a valid positive number', () => {
      const cutoff = computeDateCutoff('7');
      expect(cutoff).toBe(new Date('2024-07-25T00:00:00Z').getTime());
    });
  });

  describe('filterJobsByDate', () => {
    it('filters out jobs older than the cutoff', () => {
      const jobs: Job[] = [
        { id: '1', title: 'A', company: 'Acme', location: 'Remote', postedAt: '2024-01-01T00:00:00Z', url: '#' },
        { id: '2', title: 'B', company: 'Acme', location: 'Remote', postedAt: '2024-01-05T00:00:00Z', url: '#' },
      ];

      const cutoff = new Date('2024-01-03T00:00:00Z').getTime();
      const filtered = filterJobsByDate(jobs, cutoff);

      expect(filtered).toHaveLength(1);
      expect(filtered[0].id).toBe('2');
    });

    it('returns original array when cutoff is null', () => {
      const jobs: Job[] = [
        { id: '1', title: 'A', company: 'Acme', location: 'Remote', postedAt: '2024-01-01T00:00:00Z', url: '#' },
      ];
      expect(filterJobsByDate(jobs, null)).toBe(jobs);
    });
  });

  describe('encodeCursorValue', () => {
    const realWindow = global.window;

    beforeEach(() => {
      global.window = {
        ...(realWindow ?? {}),
        btoa: (value: string) => Buffer.from(value, 'binary').toString('base64'),
      } as unknown as Window & typeof globalThis;
    });

    afterEach(() => {
      if (realWindow) {
        global.window = realWindow;
      } else {
        // @ts-expect-error - allow cleanup when window was undefined
        delete global.window;
      }
    });

    it('encodes cursor with base64 url-safe characters', () => {
      const cursor = encodeCursorValue('2024-01-05T00:00:00Z', 42);
      expect(cursor).toBe(Buffer.from(`${new Date('2024-01-05T00:00:00Z').getTime()}:42`).toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, ''));
    });

    it('returns null when postedAt or id cannot be converted', () => {
      expect(encodeCursorValue('invalid', 42)).toBeNull();
      expect(encodeCursorValue('2024-01-05T00:00:00Z', 'abc')).toBeNull();
    });
  });

  it('exposes date filter multiplier constant', () => {
    expect(DATE_FILTER_SIZE_MULTIPLIER).toBe(5);
  });
});

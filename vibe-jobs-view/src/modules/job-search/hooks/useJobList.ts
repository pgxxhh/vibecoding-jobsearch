import { useCallback, useEffect, useMemo, useState } from 'react';
import type { Job, JobsQuery } from '@/modules/job-search/types';
import { fetchJobs } from '@/modules/job-search/services/jobSearchService';

export type JobListFilters = {
  company?: string;
  level?: string;
  remote?: string;
  salaryMin?: string;
  datePosted?: string;
  [key: string]: string | undefined;
};

export interface UseJobListOptions {
  query: string;
  location: string;
  filters: JobListFilters;
  pageSize?: number;
  onReset?: (jobs: Job[]) => void;
}

export const DEFAULT_PAGE_SIZE = 10;
export const DATE_FILTER_SIZE_MULTIPLIER = 5;

export function computeDateCutoff(daysValue: string | undefined): number | null {
  if (!daysValue) return null;
  const days = Number(daysValue);
  if (!Number.isFinite(days) || days <= 0) return null;
  return Date.now() - days * 24 * 60 * 60 * 1000;
}

export function filterJobsByDate(items: Job[], cutoff: number | null): Job[] {
  if (!cutoff) return items;
  return items.filter((item) => {
    const postedAtMillis = new Date(item.postedAt).getTime();
    return Number.isFinite(postedAtMillis) && postedAtMillis >= cutoff;
  });
}

export function encodeCursorValue(postedAt: string, id: string | number): string | null {
  const postedAtMillis = new Date(postedAt).getTime();
  const numericId = Number(id);
  if (!Number.isFinite(postedAtMillis) || !Number.isFinite(numericId)) {
    return null;
  }
  const payload = `${postedAtMillis}:${numericId}`;
  if (typeof window === 'undefined' || typeof window.btoa !== 'function') {
    return null;
  }
  try {
    const base64 = window.btoa(payload);
    return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  } catch {
    return null;
  }
}

export function useJobList({ query, location, filters, pageSize = DEFAULT_PAGE_SIZE, onReset }: UseJobListOptions) {
  const [jobs, setJobs] = useState<Job[]>([]);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(true);
  const [isLoading, setIsLoading] = useState(false);
  const [isInitialLoading, setIsInitialLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const baseParams = useMemo(() => {
    const trimmedQuery = query.trim();
    const trimmedLocation = location.trim();
    const params: JobsQuery = {
      ...filters,
    };
    if (trimmedQuery) {
      params.q = trimmedQuery;
      params.searchDetail = true;
    }
    if (trimmedLocation) {
      params.location = trimmedLocation;
    }
    return params;
  }, [filters, location, query]);

  const load = useCallback(
    async (cursor?: string | null, reset = false) => {
      setIsLoading(true);
      if (reset) {
        setIsInitialLoading(true);
      }
      try {
        setError(null);
        const cutoff = computeDateCutoff(filters.datePosted);
        const requestedSize = pageSize * (cutoff ? DATE_FILTER_SIZE_MULTIPLIER : 1);
        const paramsForFetch: JobsQuery = {
          ...baseParams,
          size: requestedSize,
        };
        if (cursor) {
          paramsForFetch.cursor = cursor;
        }

        const response = await fetchJobs(paramsForFetch);
        const filteredByDate = filterJobsByDate(response.items ?? [], cutoff);
        const filteredItems = filteredByDate.slice(0, pageSize);

        let nextCursorValue: string | null = null;
        const remainingFiltered = filteredByDate.length - filteredItems.length;
        if (remainingFiltered > 0 && filteredItems.length > 0) {
          const lastItem = filteredItems[filteredItems.length - 1];
          nextCursorValue = encodeCursorValue(lastItem.postedAt, lastItem.id) ?? response.nextCursor ?? null;
        } else if (response.nextCursor) {
          nextCursorValue = response.nextCursor;
        }

        setJobs((previous) => (reset ? filteredItems : [...previous, ...filteredItems]));
        setNextCursor(nextCursorValue);
        setHasMore(Boolean(nextCursorValue));

        if (reset) {
          onReset?.(filteredItems);
        }
      } catch (err) {
        setError(err instanceof Error ? err : new Error('Failed to load jobs'));
        if (reset) {
          setJobs([]);
        }
        setHasMore(false);
      } finally {
        setIsLoading(false);
        if (reset) {
          setIsInitialLoading(false);
        }
      }
    },
    [baseParams, filters.datePosted, onReset, pageSize],
  );

  const refresh = useCallback(() => load(undefined, true), [load]);

  const loadMore = useCallback(() => {
    if (!nextCursor || isLoading) {
      return Promise.resolve();
    }
    return load(nextCursor, false);
  }, [isLoading, load, nextCursor]);

  useEffect(() => {
    let cancelled = false;
    const bootstrap = async () => {
      setIsInitialLoading(true);
      try {
        await load(undefined, true);
      } finally {
        if (!cancelled) {
          setIsInitialLoading(false);
        }
      }
    };
    bootstrap();
    return () => {
      cancelled = true;
    };
  }, [load]);

  return {
    jobs,
    hasMore,
    isLoading,
    isInitialLoading,
    error,
    refresh,
    loadMore,
    loadNext: loadMore,
    nextCursor,
    setJobs,
  };
}

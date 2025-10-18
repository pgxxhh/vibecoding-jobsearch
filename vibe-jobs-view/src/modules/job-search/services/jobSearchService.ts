import { normalizeJobDetailFromApi, normalizeJobFromApi } from '@/modules/job-search/utils/jobs-normalization';
import type {
  Job,
  JobDetail as JobDetailData,
  JobsQuery,
  JobsResponse,
} from '@/modules/job-search/types';

const API_BASE =
  process.env.NEXT_PUBLIC_BACKEND_BASE ??
  process.env.NEXT_PUBLIC_API_BASE ??
  '/api';

function parseTotal(rawTotal: unknown, fallback: number): number | null {
  if (typeof rawTotal === 'number') {
    return Number.isFinite(rawTotal) && rawTotal >= 0 ? rawTotal : fallback;
  }
  if (typeof rawTotal === 'string' && rawTotal.trim() !== '') {
    const parsed = Number(rawTotal);
    if (Number.isFinite(parsed) && parsed >= 0) {
      return parsed;
    }
  }
  return fallback;
}

export async function fetchJobs(params: JobsQuery): Promise<JobsResponse> {
  const queryEntries = Object.entries(params)
    .filter(([, value]) => value !== '' && value !== undefined && value !== null)
    .map(([key, value]) => [key, String(value)] as [string, string]);

  const qs = new URLSearchParams(queryEntries);

  const response = await fetch(`${API_BASE}/jobs?${qs.toString()}`, { cache: 'no-store' });
  if (!response.ok) {
    throw new Error('Failed to fetch jobs');
  }

  const payload = await response.json();
  const items = Array.isArray(payload?.items)
    ? (payload.items.map(normalizeJobFromApi).filter(Boolean) as Job[])
    : [];

  return {
    items,
    total: parseTotal(payload?.total, items.length),
    nextCursor: typeof payload?.nextCursor === 'string' ? payload.nextCursor : null,
    hasMore: Boolean(payload?.hasMore),
    size: Number.isFinite(Number(payload?.size)) ? Number(payload.size) : items.length,
  };
}

export async function fetchJobDetail(id: string): Promise<JobDetailData> {
  const response = await fetch(`${API_BASE}/jobs/${id}/detail`, { cache: 'no-store' });
  if (!response.ok) {
    throw new Error('Failed to fetch job detail');
  }
  const detail = await response.json();
  return normalizeJobDetailFromApi(detail, id);
}

import type { Job, JobDetail as JobDetailData } from '@/lib/types';

function toRecord(value: unknown): Record<string, unknown> | undefined {
  if (value && typeof value === 'object' && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return undefined;
}

function toStringValue(value: unknown): string | null {
  if (typeof value === 'string') {
    const trimmed = value.trim();
    return trimmed.length > 0 ? trimmed : null;
  }
  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value);
  }
  return null;
}

function toStringArray(value: unknown): string[] {
  if (!Array.isArray(value)) return [];
  const result: string[] = [];
  for (const item of value) {
    const str = toStringValue(item);
    if (!str) continue;
    if (result.includes(str)) continue;
    result.push(str);
  }
  return result;
}

function toJsonString(value: unknown): string | null {
  if (value == null) return null;
  if (typeof value === 'string') {
    const trimmed = value.trim();
    return trimmed.length > 0 ? trimmed : null;
  }
  try {
    return JSON.stringify(value);
  } catch {
    return null;
  }
}

export function normalizeJobFromApi(item: any): Job {
  const enrichments = toRecord(item?.enrichments);
  const enrichmentStatus = toRecord(item?.enrichmentStatus) ?? toRecord(enrichments?.status);
  const statusStateRaw = enrichmentStatus && 'state' in enrichmentStatus
    ? (enrichmentStatus as Record<string, unknown>)['state']
    : undefined;
  const statusState = toStringValue(statusStateRaw)?.toUpperCase() ?? null;
  const isEnrichmentReady = statusState === 'SUCCESS';
  const summaryFromEnrichment = isEnrichmentReady && enrichments
    ? toStringValue(enrichments['summary'])
    : null;
  const summary = isEnrichmentReady
    ? summaryFromEnrichment ?? toStringValue(item?.summary) ?? undefined
    : undefined;
  const skillList = isEnrichmentReady && enrichments ? toStringArray(enrichments['skills']) : [];
  const fallbackSkills = isEnrichmentReady && Array.isArray(item?.skills) ? toStringArray(item.skills) : [];
  const skills = (skillList.length > 0 ? skillList : fallbackSkills) as string[];
  const highlightList = isEnrichmentReady && enrichments ? toStringArray(enrichments['highlights']) : [];
  const fallbackHighlights = isEnrichmentReady && Array.isArray(item?.highlights)
    ? toStringArray(item.highlights)
    : [];
  const highlights = (highlightList.length > 0 ? highlightList : fallbackHighlights) as string[];
  const structuredRaw = isEnrichmentReady && enrichments && enrichments['structured_data'] !== undefined
    ? enrichments['structured_data']
    : (isEnrichmentReady ? item?.structuredData : null);
  const structuredData = isEnrichmentReady ? toJsonString(structuredRaw) ?? undefined : undefined;
  const tags = Array.isArray(item?.tags) ? toStringArray(item.tags) : [];

  return {
    id: String(item?.id ?? ''),
    title: toStringValue(item?.title) ?? '',
    company: toStringValue(item?.company) ?? '',
    location: toStringValue(item?.location) ?? '',
    level: toStringValue(item?.level) ?? undefined,
    postedAt: toStringValue(item?.postedAt) ?? '',
    tags,
    url: toStringValue(item?.url) ?? '',
    enrichments: enrichments ?? undefined,
    enrichmentStatus: enrichmentStatus ?? undefined,
    summary: summary ?? undefined,
    skills,
    highlights,
    structuredData,
    detailMatch: Boolean(item?.detailMatch),
  };
}

export function normalizeJobDetailFromApi(detail: any, fallbackId: string): JobDetailData {
  const enrichments = toRecord(detail?.enrichments);
  const enrichmentStatus = toRecord(detail?.enrichmentStatus) ?? toRecord(enrichments?.status);
  const statusStateRaw = enrichmentStatus && 'state' in enrichmentStatus
    ? (enrichmentStatus as Record<string, unknown>)['state']
    : undefined;
  const statusState = toStringValue(statusStateRaw)?.toUpperCase() ?? null;
  const isEnrichmentReady = statusState === 'SUCCESS';
  const summary = isEnrichmentReady && enrichments ? toStringValue(enrichments['summary']) : null;
  const skills = isEnrichmentReady && enrichments ? toStringArray(enrichments['skills']) : [];
  const highlights = isEnrichmentReady && enrichments ? toStringArray(enrichments['highlights']) : [];
  const structuredRaw = isEnrichmentReady && enrichments && enrichments['structured_data'] !== undefined
    ? enrichments['structured_data']
    : (isEnrichmentReady ? detail?.structuredData : null);
  return {
    id: String(detail?.id ?? fallbackId),
    title: toStringValue(detail?.title) ?? '',
    company: toStringValue(detail?.company) ?? '',
    location: toStringValue(detail?.location) ?? '',
    postedAt: toStringValue(detail?.postedAt) ?? '',
    content: typeof detail?.content === 'string' ? detail.content : '',
    enrichments: enrichments ?? undefined,
    enrichmentStatus: enrichmentStatus ?? undefined,
    summary: isEnrichmentReady ? (summary ?? toStringValue(detail?.summary)) : undefined,
    skills: isEnrichmentReady && skills.length > 0
      ? skills
      : (isEnrichmentReady && Array.isArray(detail?.skills) ? toStringArray(detail.skills) : []),
    highlights: isEnrichmentReady && highlights.length > 0
      ? highlights
      : (isEnrichmentReady && Array.isArray(detail?.highlights) ? toStringArray(detail.highlights) : []),
    structuredData: isEnrichmentReady ? toJsonString(structuredRaw) : undefined,
  };
}

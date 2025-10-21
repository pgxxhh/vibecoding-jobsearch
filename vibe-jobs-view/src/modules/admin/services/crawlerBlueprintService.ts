import type {
  ActivateCrawlerBlueprintPayload,
  CreateCrawlerBlueprintPayload,
  CrawlerBlueprintDetail,
  CrawlerBlueprintSummary,
  RerunCrawlerBlueprintPayload,
} from '@/modules/admin/types';

async function parseJson<T>(response: Response): Promise<T> {
  const text = await response.text();
  if (!response.ok) {
    const message = text || '请求失败';
    throw new Error(message);
  }

  if (!text) {
    return undefined as T;
  }

  try {
    return JSON.parse(text) as T;
  } catch (error) {
    throw new Error('后台返回了无效的 JSON');
  }
}

function withJson(body?: unknown): RequestInit {
  if (body === undefined) {
    return {};
  }
  return {
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(body),
  };
}

export async function fetchCrawlerBlueprints(): Promise<CrawlerBlueprintSummary[]> {
  const res = await fetch('/api/admin/crawler-blueprints', { cache: 'no-store' });
  return parseJson<CrawlerBlueprintSummary[]>(res);
}

export async function fetchCrawlerBlueprintDetail(code: string): Promise<CrawlerBlueprintDetail> {
  const encoded = encodeURIComponent(code);
  const res = await fetch(`/api/admin/crawler-blueprints/${encoded}`, { cache: 'no-store' });
  return parseJson<CrawlerBlueprintDetail>(res);
}

export async function createCrawlerBlueprint(payload: CreateCrawlerBlueprintPayload) {
  const res = await fetch('/api/admin/crawler-blueprints', {
    method: 'POST',
    ...withJson(payload),
  });
  return parseJson<CrawlerBlueprintDetail>(res);
}

export async function rerunCrawlerBlueprint({ code, ...rest }: RerunCrawlerBlueprintPayload) {
  const encoded = encodeURIComponent(code);
  const payload = {
    name: rest.name?.trim() || undefined,
    entryUrl: rest.entryUrl?.trim() || undefined,
    searchKeywords: rest.searchKeywords?.trim() || undefined,
    excludeSelectors: rest.excludeSelectors?.length ? rest.excludeSelectors : undefined,
    notes: rest.notes?.trim() || undefined,
  };

  const hasPayload = Object.values(payload).some((value) => {
    if (Array.isArray(value)) {
      return value.length > 0;
    }
    return value !== undefined;
  });

  const res = await fetch(`/api/admin/crawler-blueprints/${encoded}/rerun`, {
    method: 'POST',
    ...withJson(hasPayload ? payload : {}),
  });
  return parseJson<{ taskId?: string }>(res);
}

export async function activateCrawlerBlueprint({ code, enable }: ActivateCrawlerBlueprintPayload) {
  const encoded = encodeURIComponent(code);
  const res = await fetch(`/api/admin/crawler-blueprints/${encoded}/activate`, {
    method: 'POST',
    ...withJson({ enable }),
  });
  return parseJson<CrawlerBlueprintDetail | { success?: boolean }>(res);
}

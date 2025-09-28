
import { NextRequest, NextResponse } from 'next/server';

import { MOCK_JOBS } from './mock-data';

type CursorPayload = { postedAt: number; id: number };

function decodeCursor(cursor: string | null): CursorPayload | null {
  if (!cursor) return null;
  try {
    const decoded = Buffer.from(cursor, 'base64url').toString('utf8');
    const [postedAtRaw, idRaw] = decoded.split(':');
    if (!postedAtRaw || !idRaw) return null;
    const postedAt = Number(postedAtRaw);
    const id = Number(idRaw);
    if (!Number.isFinite(postedAt) || !Number.isFinite(id)) return null;
    return { postedAt, id };
  } catch {
    return null;
  }
}

function encodeCursor(postedAt: string, id: string): string | null {
  const postedAtMillis = new Date(postedAt).getTime();
  const numericId = Number(id);
  if (!Number.isFinite(postedAtMillis) || !Number.isFinite(numericId)) {
    return null;
  }
  return Buffer.from(`${postedAtMillis}:${numericId}`, 'utf8').toString('base64url');
}

function buildBackendUrl(base: string, path: string) {
  const url = new URL(base);
  const basePath = url.pathname.replace(/\/$/, '');
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  const prefix = basePath.endsWith('/api') ? basePath : `${basePath}/api`;
  url.pathname = `${prefix}${normalizedPath}`;
  return url;
}

function resolveBackendBase(): string | null {
  const runtimeBase = process.env['BACKEND_BASE_URL'];
  if (runtimeBase && runtimeBase.trim()) {
    return runtimeBase.trim();
  }

  const publicBase = process.env['NEXT_PUBLIC_BACKEND_BASE'];
  if (publicBase) {
    const trimmed = publicBase.trim();
    if (trimmed && /^(https?:)?\/\//.test(trimmed)) {
      return trimmed;
    }
  }

  return null;
}

export async function GET(req: NextRequest) {
  const url = new URL(req.url);
  const q = url.searchParams.get('q')?.toLowerCase() ?? '';
  const company = url.searchParams.get('company')?.toLowerCase() ?? '';
  const location = url.searchParams.get('location')?.toLowerCase() ?? '';
  const level = url.searchParams.get('level')?.toLowerCase() ?? '';
  const cursorParam = url.searchParams.get('cursor');
  const cursor = decodeCursor(cursorParam);
  const datePostedRaw = url.searchParams.get('datePosted');

  let datePostedCutoff: number | null = null;
  if (datePostedRaw) {
    const days = Number(datePostedRaw);
    if (Number.isFinite(days) && days > 0) {
      const cutoff = Date.now() - days * 24 * 60 * 60 * 1000;
      datePostedCutoff = Math.max(0, Math.floor(cutoff));
    }
  }

  const sizeParam = Number(url.searchParams.get('size') ?? '10');
  const size = Math.max(1, Math.min(Number.isFinite(sizeParam) ? sizeParam : 10, 100));

  if (cursorParam && !cursor) {
    return NextResponse.json({ error: 'Invalid cursor' }, { status: 400 });
  }

  const base = resolveBackendBase();
  if (base) {
    const upstream = buildBackendUrl(base, '/jobs');
    for (const [k, v] of url.searchParams.entries()) upstream.searchParams.set(k, v);
    const r = await fetch(upstream, { headers: { accept: 'application/json' } });
    const text = await r.text();
    try {
      const json = JSON.parse(text);
      return NextResponse.json(json, { status: r.status });
    } catch {
      return NextResponse.json({ error: 'Invalid JSON from backend', raw: text }, { status: 502 });
    }
  }

  const filtered = MOCK_JOBS.filter(j =>
    (!q || [j.title, (j.tags ?? []).join(' ')].join(' ').toLowerCase().includes(q)) &&
    (!company || j.company.toLowerCase().includes(company)) &&
    (!location || j.location.toLowerCase().includes(location)) &&
    (!level || (j.level ?? '').toLowerCase() === level) &&
    (!datePostedCutoff || new Date(j.postedAt).getTime() >= datePostedCutoff)
  );

  const sorted = [...filtered].sort((a, b) => {
    const aTime = new Date(a.postedAt).getTime();
    const bTime = new Date(b.postedAt).getTime();
    if (aTime !== bTime) return bTime - aTime;
    return Number(b.id) - Number(a.id);
  });

  const window = cursor
    ? sorted.filter(job => {
        const postedAt = new Date(job.postedAt).getTime();
        const jobId = Number(job.id);
        if (!Number.isFinite(postedAt) || !Number.isFinite(jobId)) return false;
        if (postedAt < cursor.postedAt) return true;
        if (postedAt === cursor.postedAt) return jobId < cursor.id;
        return false;
      })
    : sorted;

  const pageWindow = window.slice(0, size + 1);
  const hasMore = pageWindow.length > size;
  const items = pageWindow.slice(0, size);
  const lastItem = items.at(-1) ?? null;
  const nextCursorValue = hasMore && lastItem ? encodeCursor(lastItem.postedAt, lastItem.id) : null;

  return NextResponse.json({
    items,
    total: filtered.length,
    nextCursor: nextCursorValue,
    hasMore: hasMore && Boolean(nextCursorValue),
    size,
  });
}


import { NextRequest, NextResponse } from 'next/server';

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
  const base = resolveBackendBase();
  if (!base) {
    return NextResponse.json({ error: 'Backend base URL not configured' }, { status: 500 });
  }

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

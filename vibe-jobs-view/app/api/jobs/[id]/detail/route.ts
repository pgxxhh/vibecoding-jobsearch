import { NextRequest, NextResponse } from 'next/server';

type Params = { params: { id: string } };

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

  const candidates = [
    process.env['NEXT_PUBLIC_BACKEND_BASE'],
    process.env['NEXT_PUBLIC_API_BASE'],
  ];

  for (const candidate of candidates) {
    if (!candidate) continue;
    const trimmed = candidate.trim();
    if (trimmed && /^(https?:)?\/\//.test(trimmed)) {
      return trimmed;
    }
  }

  return null;
}

export async function GET(_req: NextRequest, { params }: Params) {
  const { id } = params;
  const base = resolveBackendBase();

  if (!base) {
    return NextResponse.json({ error: 'Backend base URL not configured' }, { status: 500 });
  }

  const upstream = buildBackendUrl(base, `/jobs/${id}/detail`);
  const res = await fetch(upstream, { headers: { accept: 'application/json' } });
  const text = await res.text();

  try {
    const json = JSON.parse(text);
    const content = json.content ?? json.description ?? '';
    return NextResponse.json(
      {
        id: String(json.id ?? id),
        title: json.title ?? '',
        company: json.company ?? '',
        location: json.location ?? '',
        postedAt: json.postedAt ?? '',
        content,
      },
      { status: res.status },
    );
  } catch {
    return NextResponse.json({ error: 'Invalid JSON from backend', raw: text }, { status: 502 });
  }
}

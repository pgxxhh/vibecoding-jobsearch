import { NextRequest, NextResponse } from 'next/server';

import { buildBackendUrl, resolveBackendBase } from '@/shared/lib/backend';

async function forward(request: NextRequest, path: string, init?: RequestInit) {
  const base = resolveBackendBase(request);
  if (!base) {
    return NextResponse.json({ error: 'Backend base URL not configured' }, { status: 500 });
  }

  const upstream = buildBackendUrl(base, path);
  const headers = new Headers(init?.headers);
  headers.set('accept', 'application/json');
  const cookie = request.headers.get('cookie');
  if (cookie) {
    headers.set('cookie', cookie);
  }

  const response = await fetch(upstream, {
    method: init?.method ?? request.method,
    headers,
    body: init?.body,
    cache: 'no-store',
  });

  const text = await response.text();
  if (!text) {
    return NextResponse.json(null, { status: response.status });
  }

  try {
    const json = JSON.parse(text);
    return NextResponse.json(json, { status: response.status });
  } catch {
    return NextResponse.json({ error: 'Invalid JSON from backend', raw: text }, { status: 502 });
  }
}

export async function GET(request: NextRequest, { params }: { params: { code: string } }) {
  const encoded = encodeURIComponent(params.code);
  return forward(request, `/admin/crawler-blueprints/${encoded}`);
}

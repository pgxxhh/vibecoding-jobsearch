import { NextRequest, NextResponse } from 'next/server';

import { buildBackendUrl, resolveBackendBase } from '@/shared/lib/backend';

export async function POST(request: NextRequest, { params }: { params: { code: string } }) {
  const base = resolveBackendBase(request);
  if (!base) {
    return NextResponse.json({ error: 'Backend base URL not configured' }, { status: 500 });
  }

  const encoded = encodeURIComponent(params.code);
  const upstream = buildBackendUrl(base, `/admin/crawler-blueprints/${encoded}/activate`);
  const payload = await request.json().catch(() => null);

  const headers = new Headers({ 'content-type': 'application/json', accept: 'application/json' });
  const cookie = request.headers.get('cookie');
  if (cookie) {
    headers.set('cookie', cookie);
  }

  const response = await fetch(upstream, {
    method: 'POST',
    headers,
    body: JSON.stringify(payload ?? {}),
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

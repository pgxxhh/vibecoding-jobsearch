import { NextRequest, NextResponse } from 'next/server';
import { buildBackendUrl, resolveBackendBase } from '@/lib/backend';

export async function POST(req: NextRequest) {
  const base = resolveBackendBase(req);
  if (!base) {
    return NextResponse.json({ code: 'CONFIG_ERROR', message: 'Backend base URL not configured.' }, { status: 500 });
  }
  const segments = req.nextUrl.pathname.split('/');
  const id = segments[segments.length - 2];
  const upstream = buildBackendUrl(base, `/subscriptions/${id}/unsubscribe`);
  const body = await req.text();
  const response = await fetch(upstream, {
    method: 'POST',
    headers: {
      accept: 'application/json',
      'content-type': 'application/json',
    },
    body,
    cache: 'no-store',
  });
  const text = await response.text();
  if (text) {
    try {
      const json = JSON.parse(text);
      return NextResponse.json(json, { status: response.status });
    } catch {
      return NextResponse.json({ code: 'UPSTREAM_ERROR', message: 'Unexpected response from backend.', raw: text }, { status: 502 });
    }
  }
  return new NextResponse(null, { status: response.status });
}

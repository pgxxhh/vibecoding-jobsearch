import { NextRequest, NextResponse } from 'next/server';
import { buildBackendUrl, resolveBackendBase } from '@/lib/backend';

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

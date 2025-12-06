import { NextRequest, NextResponse } from 'next/server';
import { buildBackendUrl, resolveBackendBase } from '@/shared/lib/backend';

export async function POST(req: NextRequest, { params }: { params: { id: string } }) {
  const base = resolveBackendBase(req);
  if (!base) {
    return NextResponse.json({ error: 'Backend base URL not configured' }, { status: 500 });
  }
  const payload = await req.text();
  const upstream = buildBackendUrl(base, `/resumes/${params.id}/feedback`);
  const response = await fetch(upstream, {
    method: 'POST',
    headers: { 'content-type': req.headers.get('content-type') || 'application/json' },
    body: payload,
  });
  const text = await response.text();
  try {
    const json = text ? JSON.parse(text) : {};
    return NextResponse.json(json, { status: response.status });
  } catch {
    return NextResponse.json({ error: 'Unexpected response from backend', raw: text }, { status: 502 });
  }
}

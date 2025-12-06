import { NextRequest, NextResponse } from 'next/server';
import { buildBackendUrl, resolveBackendBase } from '@/shared/lib/backend';

export async function GET(req: NextRequest, { params }: { params: { id: string } }) {
  const base = resolveBackendBase(req);
  if (!base) {
    return NextResponse.json({ error: 'Backend base URL not configured' }, { status: 500 });
  }
  const upstream = buildBackendUrl(base, `/resumes/${params.id}/recommendations`);
  const searchParams = new URL(req.url).searchParams;
  searchParams.forEach((value, key) => upstream.searchParams.set(key, value));

  const response = await fetch(upstream, { headers: { accept: 'application/json' }, cache: 'no-store' });
  const text = await response.text();
  try {
    const json = text ? JSON.parse(text) : {};
    return NextResponse.json(json, { status: response.status });
  } catch {
    return NextResponse.json({ error: 'Unexpected response from backend', raw: text }, { status: 502 });
  }
}

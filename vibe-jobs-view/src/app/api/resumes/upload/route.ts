import { NextRequest, NextResponse } from 'next/server';
import { buildBackendUrl, resolveBackendBase } from '@/shared/lib/backend';

export async function POST(req: NextRequest) {
  const base = resolveBackendBase(req);
  if (!base) {
    return NextResponse.json({ error: 'Backend base URL not configured' }, { status: 500 });
  }
  const formData = await req.formData();
  const upstream = buildBackendUrl(base, '/resumes/upload');
  const response = await fetch(upstream, {
    method: 'POST',
    body: formData,
  });
  const text = await response.text();
  try {
    const json = text ? JSON.parse(text) : {};
    return NextResponse.json(json, { status: response.status });
  } catch {
    return NextResponse.json({ error: 'Unexpected response from backend', raw: text }, { status: 502 });
  }
}

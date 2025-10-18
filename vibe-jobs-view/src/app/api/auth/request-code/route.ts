import { NextRequest, NextResponse } from 'next/server';
import { buildBackendUrl, resolveBackendBase } from '@/shared/lib/backend';

export async function POST(req: NextRequest) {
  let email: string | null = null;
  try {
    const payload = await req.json();
    email = typeof payload?.email === 'string' ? payload.email.trim() : null;
  } catch {
    return NextResponse.json({ code: 'INVALID_PAYLOAD', message: 'Invalid request body.' }, { status: 400 });
  }

  if (!email) {
    return NextResponse.json({ code: 'INVALID_EMAIL', message: 'Email is required.' }, { status: 400 });
  }

  const base = resolveBackendBase(req);
  if (!base) {
    return NextResponse.json({ code: 'CONFIG_ERROR', message: 'Backend base URL not configured.' }, { status: 500 });
  }

  const upstream = buildBackendUrl(base, '/auth/challenges');
  const response = await fetch(upstream, {
    method: 'POST',
    headers: { 'content-type': 'application/json', accept: 'application/json' },
    cache: 'no-store',
    body: JSON.stringify({ email }),
  });

  const text = await response.text();
  try {
    const json = JSON.parse(text);
    return NextResponse.json(json, { status: response.status });
  } catch {
    return NextResponse.json({ code: 'UPSTREAM_ERROR', message: 'Unexpected response from backend.', raw: text }, { status: 502 });
  }
}

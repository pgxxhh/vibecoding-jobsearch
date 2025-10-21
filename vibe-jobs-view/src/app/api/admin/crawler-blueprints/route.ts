import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';

import { buildBackendUrl, resolveBackendBase } from '@/shared/lib/backend';

function resolveToken(request: NextRequest): string | null {
  const headerToken = request.headers.get('x-session-token');
  const cookieToken = cookies().get('vj_session')?.value;
  const token = headerToken?.trim() || cookieToken?.trim();
  return token && token.length > 0 ? token : null;
}

async function forward(request: NextRequest, path: string, init?: RequestInit) {
  const token = resolveToken(request);
  if (!token) {
    return NextResponse.json({ code: 'NO_SESSION', message: 'Admin session required' }, { status: 401 });
  }

  const base = resolveBackendBase(request);
  if (!base) {
    return NextResponse.json({ error: 'Backend base URL not configured' }, { status: 500 });
  }

  const upstream = buildBackendUrl(base, path);
  const searchParams = request.nextUrl.searchParams;
  searchParams.forEach((value, key) => {
    upstream.searchParams.set(key, value);
  });

  const headers = new Headers(init?.headers);
  headers.set('accept', 'application/json');
  headers.set('x-session-token', token);
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

export async function GET(request: NextRequest) {
  return forward(request, '/admin/crawler-blueprints');
}

export async function POST(request: NextRequest) {
  const payload = await request.json().catch(() => null);
  return forward(request, '/admin/crawler-blueprints', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(payload ?? {}),
  });
}

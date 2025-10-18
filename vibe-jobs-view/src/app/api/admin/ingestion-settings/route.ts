import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';
import { buildBackendUrl, resolveBackendBase } from '@/shared/lib/backend';

function resolveSessionToken(req: NextRequest): string | null {
  const headerToken = req.headers.get('x-session-token');
  const cookieToken = cookies().get('vj_session')?.value;
  const token = headerToken?.trim() || cookieToken?.trim();
  return token && token.length > 0 ? token : null;
}

async function proxy(req: NextRequest, method: 'GET' | 'PUT') {
  const token = resolveSessionToken(req);
  if (!token) {
    return NextResponse.json({ code: 'NO_SESSION', message: 'Admin session required' }, { status: 401 });
  }

  const base = resolveBackendBase(req);
  if (!base) {
    return NextResponse.json({ code: 'CONFIG_ERROR', message: 'Backend base URL not configured' }, { status: 500 });
  }

  const upstream = buildBackendUrl(base, '/admin/ingestion-settings');
  const headers: Record<string, string> = {
    accept: 'application/json',
    'x-session-token': token,
  };

  let body: string | undefined;
  if (method === 'PUT') {
    body = await req.text();
    headers['content-type'] = req.headers.get('content-type') || 'application/json';
  }

  const response = await fetch(upstream, {
    method,
    headers,
    body,
    cache: 'no-store',
  });

  const text = await response.text();
  try {
    const json = text ? JSON.parse(text) : null;
    return NextResponse.json(json, { status: response.status });
  } catch {
    return NextResponse.json({ code: 'UPSTREAM_ERROR', message: 'Unexpected response from backend', raw: text }, { status: 502 });
  }
}

export async function GET(req: NextRequest) {
  return proxy(req, 'GET');
}

export async function PUT(req: NextRequest) {
  return proxy(req, 'PUT');
}

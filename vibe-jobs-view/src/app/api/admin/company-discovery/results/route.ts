import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';
import { buildBackendUrl, resolveBackendBase } from '@/shared/lib/backend';

function resolveSessionToken(req: NextRequest): string | null {
  const headerToken = req.headers.get('x-session-token');
  const cookieToken = cookies().get('vj_session')?.value;
  const token = headerToken?.trim() || cookieToken?.trim();
  return token && token.length > 0 ? token : null;
}

export async function GET(req: NextRequest) {
  const token = resolveSessionToken(req);
  if (!token) {
    return NextResponse.json({ code: 'NO_SESSION', message: 'Admin session required' }, { status: 401 });
  }

  const base = resolveBackendBase(req);
  if (!base) {
    return NextResponse.json({ code: 'CONFIG_ERROR', message: 'Backend base URL not configured' }, { status: 500 });
  }

  const upstream = buildBackendUrl(base, '/admin/company-discovery/results');
  const response = await fetch(upstream, {
    method: 'GET',
    headers: {
      accept: 'application/json',
      'x-session-token': token,
    },
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

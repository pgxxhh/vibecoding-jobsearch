import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';
import { buildBackendUrl, resolveBackendBase } from '@/lib/backend';

function resolveToken(req: NextRequest): string | null {
  const headerToken = req.headers.get('x-session-token');
  const cookieToken = cookies().get('vj_session')?.value;
  const token = headerToken?.trim() || cookieToken?.trim();
  return token && token.length > 0 ? token : null;
}

export async function POST(req: NextRequest, context: { params: { codeOrId: string } }) {
  const token = resolveToken(req);
  if (!token) {
    return NextResponse.json({ code: 'NO_SESSION', message: 'Admin session required' }, { status: 401 });
  }

  const base = resolveBackendBase();
  if (!base) {
    return NextResponse.json({ code: 'CONFIG_ERROR', message: 'Backend base URL not configured' }, { status: 500 });
  }

  const upstream = buildBackendUrl(base, `/admin/data-sources/${context.params.codeOrId}/companies/bulk`);
  const headers: Record<string, string> = {
    accept: 'application/json',
    'content-type': 'application/json',
    'x-session-token': token,
  };

  const body = await req.text();
  
  const response = await fetch(upstream, {
    method: 'POST',
    headers,
    body,
    cache: 'no-store',
  });

  const text = await response.text();
  try {
    const json = text ? JSON.parse(text) : null;
    return NextResponse.json(json, { status: response.status });
  } catch {
    return NextResponse.json({ 
      code: 'UPSTREAM_ERROR', 
      message: 'Unexpected response from backend', 
      raw: text 
    }, { status: 502 });
  }
}
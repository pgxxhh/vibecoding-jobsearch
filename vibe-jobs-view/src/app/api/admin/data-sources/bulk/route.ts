import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';
import { buildBackendUrl, resolveBackendBase } from '@/shared/lib/backend';

function resolveToken(req: NextRequest): string | null {
  const headerToken = req.headers.get('x-session-token');
  const cookieToken = cookies().get('vj_session')?.value;
  const token = headerToken?.trim() || cookieToken?.trim();
  return token && token.length > 0 ? token : null;
}

export async function POST(req: NextRequest) {
  const token = resolveToken(req);
  if (!token) {
    return NextResponse.json({ code: 'NO_SESSION', message: 'Admin session required' }, { status: 401 });
  }
  
  const base = resolveBackendBase(req);
  if (!base) {
    return NextResponse.json({ code: 'CONFIG_ERROR', message: 'Backend base URL not configured' }, { status: 500 });
  }

  try {
    const body = await req.json();
    const { dataSources } = body;
    
    if (!Array.isArray(dataSources)) {
      return NextResponse.json({ code: 'VALIDATION_ERROR', message: 'dataSources must be an array' }, { status: 400 });
    }

    const upstream = buildBackendUrl(base, '/admin/data-sources/bulk');
    const response = await fetch(upstream, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-session-token': token,
      },
      body: JSON.stringify({ dataSources }),
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
  } catch (error) {
    return NextResponse.json({ 
      code: 'REQUEST_ERROR', 
      message: error instanceof Error ? error.message : 'Invalid request' 
    }, { status: 400 });
  }
}
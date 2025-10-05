import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';
import { buildBackendUrl, resolveBackendBase } from '@/lib/backend';

function resolveToken(req: NextRequest): string | null {
  const headerToken = req.headers.get('x-session-token');
  const cookieToken = cookies().get('vj_session')?.value;
  const token = headerToken?.trim() || cookieToken?.trim();
  return token && token.length > 0 ? token : null;
}

async function forward(req: NextRequest, method: 'GET' | 'POST' | 'PUT' | 'DELETE') {
  const token = resolveToken(req);
  if (!token) {
    return NextResponse.json({ code: 'NO_SESSION', message: 'Admin session required' }, { status: 401 });
  }
  const base = resolveBackendBase(req);
  if (!base) {
    return NextResponse.json({ code: 'CONFIG_ERROR', message: 'Backend base URL not configured' }, { status: 500 });
  }
  
  let apiPath = '/admin/data-sources';
  
  // Handle ID-based operations via query parameters
  if (method === 'PUT' || method === 'DELETE') {
    const id = req.nextUrl.searchParams.get('id');
    if (!id) {
      return NextResponse.json({ code: 'MISSING_ID', message: 'ID parameter required for PUT/DELETE' }, { status: 400 });
    }
    apiPath = `/admin/data-sources/${id}`;
  }
  
  const upstream = buildBackendUrl(base, apiPath);
  const headers: Record<string, string> = {
    accept: 'application/json',
    'x-session-token': token,
  };
  let body: string | undefined;
  if (method === 'POST' || method === 'PUT') {
    body = await req.text();
    headers['content-type'] = req.headers.get('content-type') || 'application/json';
  }
  const response = await fetch(upstream, {
    method,
    headers,
    body,
    cache: 'no-store',
  });
  
  if (method === 'DELETE') {
    return NextResponse.json(null, { status: response.status });
  }
  
  const text = await response.text();
  try {
    const json = text ? JSON.parse(text) : null;
    return NextResponse.json(json, { status: response.status });
  } catch {
    return NextResponse.json({ code: 'UPSTREAM_ERROR', message: 'Unexpected response from backend', raw: text }, { status: 502 });
  }
}

export async function GET(req: NextRequest) {
  return forward(req, 'GET');
}

export async function POST(req: NextRequest) {
  return forward(req, 'POST');
}

export async function PUT(req: NextRequest) {
  return forward(req, 'PUT');
}

export async function DELETE(req: NextRequest) {
  return forward(req, 'DELETE');
}

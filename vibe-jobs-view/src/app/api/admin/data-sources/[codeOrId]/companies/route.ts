import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';
import { buildBackendUrl, resolveBackendBase } from '@/shared/lib/backend';

function resolveToken(req: NextRequest): string | null {
  const headerToken = req.headers.get('x-session-token');
  const cookieToken = cookies().get('vj_session')?.value;
  const token = headerToken?.trim() || cookieToken?.trim();
  return token && token.length > 0 ? token : null;
}

async function forward(req: NextRequest, params: { codeOrId: string }, method: 'GET' | 'POST' | 'PUT' | 'DELETE') {
  const token = resolveToken(req);
  if (!token) {
    return NextResponse.json({ code: 'NO_SESSION', message: 'Admin session required' }, { status: 401 });
  }
  const base = resolveBackendBase(req);
  if (!base) {
    return NextResponse.json({ code: 'CONFIG_ERROR', message: 'Backend base URL not configured' }, { status: 500 });
  }
  
  let apiPath = `/admin/data-sources/${params.codeOrId}/companies`;
  
  // Handle company ID-based operations via query parameters
  if (method === 'PUT' || method === 'DELETE') {
    const companyId = req.nextUrl.searchParams.get('companyId');
    if (!companyId) {
      return NextResponse.json({ code: 'MISSING_COMPANY_ID', message: 'companyId parameter required for PUT/DELETE' }, { status: 400 });
    }
    apiPath = `/admin/data-sources/${params.codeOrId}/companies/${companyId}`;
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
    // For DELETE requests, return appropriate response based on status
    if (response.status === 204) {
      // 204 No Content - successful deletion
      return new NextResponse(null, { status: 204 });
    } else {
      // Other status codes, return them as-is
      return new NextResponse(null, { status: response.status });
    }
  }
  
  const text = await response.text();
  try {
    const json = text ? JSON.parse(text) : null;
    return NextResponse.json(json, { status: response.status });
  } catch {
    return NextResponse.json({ code: 'UPSTREAM_ERROR', message: 'Unexpected response from backend', raw: text }, { status: 502 });
  }
}

export async function GET(req: NextRequest, { params }: { params: { codeOrId: string } }) {
  return forward(req, params, 'GET');
}

export async function POST(req: NextRequest, { params }: { params: { codeOrId: string } }) {
  return forward(req, params, 'POST');
}

export async function PUT(req: NextRequest, { params }: { params: { codeOrId: string } }) {
  return forward(req, params, 'PUT');
}

export async function DELETE(req: NextRequest, { params }: { params: { codeOrId: string } }) {
  return forward(req, params, 'DELETE');
}
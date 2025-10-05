import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';
import { buildBackendUrl, resolveBackendBase } from '@/lib/backend';

function resolveToken(req: NextRequest): string | null {
  const headerToken = req.headers.get('x-session-token');
  const cookieToken = cookies().get('vj_session')?.value;
  const token = headerToken?.trim() || cookieToken?.trim();
  return token && token.length > 0 ? token : null;
}

function cloneUpstreamHeaders(response: Response): Headers {
  const headers = new Headers();
  response.headers.forEach((value, key) => {
    if (key.toLowerCase() === 'content-length') {
      return;
    }
    headers.set(key, value);
  });
  return headers;
}

function isNoBodyStatus(status: number): boolean {
  return status === 101 || status === 204 || status === 205 || status === 304;
}

async function forward(req: NextRequest, params: { codeOrId: string }, method: 'GET' | 'PUT' | 'DELETE') {
  const token = resolveToken(req);
  if (!token) {
    return NextResponse.json({ code: 'NO_SESSION', message: 'Admin session required' }, { status: 401 });
  }
  const base = resolveBackendBase(req);
  if (!base) {
    return NextResponse.json({ code: 'CONFIG_ERROR', message: 'Backend base URL not configured' }, { status: 500 });
  }

  // For PUT and DELETE, we assume the codeOrId is an ID (number)
  // For GET, we assume it's a code (string)
  const upstream = buildBackendUrl(base, `/admin/data-sources/${params.codeOrId}`);

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
  const responseHeaders = cloneUpstreamHeaders(response);
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
    return NextResponse.json(json, { status: response.status, headers: responseHeaders });
  } catch {
    return NextResponse.json({ code: 'UPSTREAM_ERROR', message: 'Unexpected response from backend', raw: text }, { status: 502 });
  }
}

export async function GET(req: NextRequest, { params }: { params: { codeOrId: string } }) {
  return forward(req, params, 'GET');
}

export async function PUT(req: NextRequest, { params }: { params: { codeOrId: string } }) {
  return forward(req, params, 'PUT');
}

export async function DELETE(req: NextRequest, { params }: { params: { codeOrId: string } }) {
  return forward(req, params, 'DELETE');
}

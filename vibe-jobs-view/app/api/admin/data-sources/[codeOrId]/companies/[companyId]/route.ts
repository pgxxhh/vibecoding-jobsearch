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

async function forward(req: NextRequest, params: { codeOrId: string; companyId: string }, method: 'GET' | 'PUT' | 'DELETE') {
  const token = resolveToken(req);
  if (!token) {
    return NextResponse.json({ code: 'NO_SESSION', message: 'Admin session required' }, { status: 401 });
  }
  const base = resolveBackendBase(req);
  if (!base) {
    return NextResponse.json({ code: 'CONFIG_ERROR', message: 'Backend base URL not configured' }, { status: 500 });
  }
  const upstream = buildBackendUrl(base, `/admin/data-sources/${params.codeOrId}/companies/${params.companyId}`);
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
    if (isNoBodyStatus(response.status)) {
      return new NextResponse(null, { status: response.status, headers: responseHeaders });
    }
    const text = await response.text();
    const contentType = responseHeaders.get('content-type') ?? '';
    if (contentType.includes('application/json')) {
      try {
        const json = text ? JSON.parse(text) : null;
        return NextResponse.json(json, { status: response.status, headers: responseHeaders });
      } catch {
        return NextResponse.json(
          { code: 'UPSTREAM_ERROR', message: 'Unexpected response from backend', raw: text },
          { status: 502 }
        );
      }
    }
    return new NextResponse(text, { status: response.status, headers: responseHeaders });
  }
  const text = await response.text();
  try {
    const json = text ? JSON.parse(text) : null;
    return NextResponse.json(json, { status: response.status, headers: responseHeaders });
  } catch {
    return NextResponse.json({ code: 'UPSTREAM_ERROR', message: 'Unexpected response from backend', raw: text }, { status: 502 });
  }
}

export async function GET(req: NextRequest, context: { params: { codeOrId: string; companyId: string } }) {
  return forward(req, context.params, 'GET');
}

export async function PUT(req: NextRequest, context: { params: { codeOrId: string; companyId: string } }) {
  return forward(req, context.params, 'PUT');
}

export async function DELETE(req: NextRequest, context: { params: { codeOrId: string; companyId: string } }) {
  return forward(req, context.params, 'DELETE');
}
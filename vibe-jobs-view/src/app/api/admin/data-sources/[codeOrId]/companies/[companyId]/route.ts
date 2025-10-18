import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';
import { buildBackendUrl, resolveBackendBase } from '@/shared/lib/backend';

type HandlerContext = { params: { codeOrId: string; companyId: string } };

type ForwardMethod = 'GET' | 'PUT' | 'DELETE';

function resolveToken(req: NextRequest): string | null {
  const headerToken = req.headers.get('x-session-token');
  const cookieToken = cookies().get('vj_session')?.value;
  const token = headerToken?.trim() || cookieToken?.trim();
  return token && token.length > 0 ? token : null;
}

async function forward(method: ForwardMethod, req: NextRequest, context: HandlerContext) {
  const token = resolveToken(req);
  if (!token) {
    return NextResponse.json({ code: 'NO_SESSION', message: 'Admin session required' }, { status: 401 });
  }

  const base = resolveBackendBase(req);
  if (!base) {
    return NextResponse.json({ code: 'CONFIG_ERROR', message: 'Backend base URL not configured' }, { status: 500 });
  }

  const { codeOrId, companyId } = context.params;
  if (!companyId) {
    return NextResponse.json({ code: 'MISSING_COMPANY_ID', message: 'Company ID required' }, { status: 400 });
  }

  const upstream = buildBackendUrl(base, `/admin/data-sources/${codeOrId}/companies/${companyId}`);
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

  if (method === 'DELETE') {
    if (response.status === 204) {
      return new NextResponse(null, { status: 204 });
    }
    return new NextResponse(null, { status: response.status });
  }

  const text = await response.text();
  try {
    const json = text ? JSON.parse(text) : null;
    return NextResponse.json(json, { status: response.status });
  } catch {
    return NextResponse.json({
      code: 'UPSTREAM_ERROR',
      message: 'Unexpected response from backend',
      raw: text,
    }, { status: 502 });
  }
}

export async function GET(req: NextRequest, context: HandlerContext) {
  return forward('GET', req, context);
}

export async function PUT(req: NextRequest, context: HandlerContext) {
  return forward('PUT', req, context);
}

export async function DELETE(req: NextRequest, context: HandlerContext) {
  return forward('DELETE', req, context);
}

import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';
import { buildBackendUrl, resolveBackendBase } from '@/shared/lib/backend';

function resolveToken(req: NextRequest): string | null {
  const headerToken = req.headers.get('x-session-token');
  const cookieToken = cookies().get('vj_session')?.value;
  const token = headerToken?.trim() || cookieToken?.trim();
  return token && token.length > 0 ? token : null;
}

export async function GET(req: NextRequest, context: { params: { codeOrId: string } }) {
  const token = resolveToken(req);
  if (!token) {
    return NextResponse.json(
      { code: 'NO_SESSION', message: 'Admin session required' },
      { status: 401 }
    );
  }

  const base = resolveBackendBase(req);
  if (!base) {
    return NextResponse.json(
      { code: 'NO_BACKEND_BASE', message: 'Backend base URL is not configured' },
      { status: 500 }
    );
  }

  try {
    const { searchParams } = new URL(req.url);
    const page = searchParams.get('page') || '0';
    const size = searchParams.get('size') || '20';

    const upstream = buildBackendUrl(base, `/admin/data-sources/${context.params.codeOrId}/paged`);
    upstream.searchParams.set('page', page);
    upstream.searchParams.set('size', size);

    const response = await fetch(upstream, {
      method: 'GET',
      headers: {
        accept: 'application/json',
        'x-session-token': token,
      },
      cache: 'no-store',
    });

    const responseText = await response.text();
    const json = responseText ? JSON.parse(responseText) : null;

    return NextResponse.json(json, { status: response.status });
  } catch (error) {
    console.error('Failed to proxy admin data source companies request', error);
    return NextResponse.json(
      { code: 'UPSTREAM_ERROR', message: 'Unable to reach backend service' },
      { status: 502 }
    );
  }
}


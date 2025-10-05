import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';
import { buildBackendUrl, resolveBackendBase } from '@/lib/backend';

function resolveSessionToken(req: NextRequest): string | undefined {
  const header = req.headers.get('x-session-token');
  if (header && header.trim().length > 0) {
    return header.trim();
  }
  const bearer = req.headers.get('authorization');
  if (bearer?.startsWith('Bearer ')) {
    const token = bearer.substring(7).trim();
    if (token) return token;
  }
  const cookieToken = cookies().get('vj_session')?.value;
  if (cookieToken && cookieToken.trim().length > 0) {
    return cookieToken.trim();
  }
  return undefined;
}

async function forward(req: NextRequest, init?: RequestInit) {
  const base = resolveBackendBase(req);
  if (!base) {
    return NextResponse.json({ code: 'CONFIG_ERROR', message: 'Backend base URL not configured.' }, { status: 500 });
  }
  const upstream = buildBackendUrl(base, `/subscriptions/${req.nextUrl.pathname.split('/').pop()}`);
  const token = resolveSessionToken(req);
  const headers: HeadersInit = {
    accept: 'application/json',
    'content-type': 'application/json',
  };
  if (token) {
    headers['x-session-token'] = token;
    headers['authorization'] = `Bearer ${token}`;
  }
  const response = await fetch(upstream, {
    method: req.method,
    headers,
    body: init?.body,
    cache: 'no-store',
  });
  const text = await response.text();
  if (text) {
    try {
      const json = JSON.parse(text);
      return NextResponse.json(json, { status: response.status });
    } catch {
      return NextResponse.json({ code: 'UPSTREAM_ERROR', message: 'Unexpected response from backend.', raw: text }, { status: 502 });
    }
  }
  return new NextResponse(null, { status: response.status });
}

export async function PATCH(req: NextRequest) {
  const body = await req.text();
  return forward(req, { body });
}

export async function DELETE(req: NextRequest) {
  return forward(req);
}

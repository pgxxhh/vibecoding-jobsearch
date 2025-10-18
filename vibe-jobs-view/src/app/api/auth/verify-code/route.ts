import { NextRequest, NextResponse } from 'next/server';
import { buildBackendUrl, resolveBackendBase } from '@/shared/lib/backend';

export async function POST(req: NextRequest) {
  let challengeId: string | null = null;
  let code: string | null = null;
  try {
    const payload = await req.json();
    challengeId = typeof payload?.challengeId === 'string' ? payload.challengeId.trim() : null;
    code = typeof payload?.code === 'string' ? payload.code.trim() : null;
  } catch {
    return NextResponse.json({ code: 'INVALID_PAYLOAD', message: 'Invalid request body.' }, { status: 400 });
  }

  if (!challengeId) {
    return NextResponse.json({ code: 'INVALID_CHALLENGE', message: 'challengeId is required.' }, { status: 400 });
  }
  if (!code) {
    return NextResponse.json({ code: 'INVALID_CODE', message: 'Verification code is required.' }, { status: 400 });
  }

  const base = resolveBackendBase(req);
  if (!base) {
    return NextResponse.json({ code: 'CONFIG_ERROR', message: 'Backend base URL not configured.' }, { status: 500 });
  }

  const upstream = buildBackendUrl(base, `/auth/challenges/${encodeURIComponent(challengeId)}/verify`);
  const response = await fetch(upstream, {
    method: 'POST',
    headers: { 'content-type': 'application/json', accept: 'application/json' },
    cache: 'no-store',
    body: JSON.stringify({ code }),
  });

  const text = await response.text();
  let json: any;
  try {
    json = JSON.parse(text);
  } catch {
    return NextResponse.json({ code: 'UPSTREAM_ERROR', message: 'Unexpected response from backend.', raw: text }, { status: 502 });
  }

  const res = NextResponse.json(json, { status: response.status });

  if (response.ok && json?.sessionToken) {
    const expiresAt = typeof json.sessionExpiresAt === 'string' ? new Date(json.sessionExpiresAt) : null;
    res.cookies.set('vj_session', json.sessionToken, {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'lax',
      path: '/',
      expires: expiresAt && !Number.isNaN(expiresAt.getTime()) ? expiresAt : undefined,
    });
  }

  return res;
}

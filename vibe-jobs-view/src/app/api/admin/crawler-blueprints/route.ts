import { NextRequest } from 'next/server';

import { forward } from './proxy';

export async function GET(request: NextRequest) {
  return forward(request, '/admin/crawler-blueprints');
}

export async function POST(request: NextRequest) {
  const payload = await request.json().catch(() => null);
  return forward(request, '/admin/crawler-blueprints', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(payload ?? {}),
  });
}

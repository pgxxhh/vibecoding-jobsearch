import { NextRequest } from 'next/server';

import { forward } from '../../proxy';

export async function POST(request: NextRequest, { params }: { params: { code: string } }) {
  const encoded = encodeURIComponent(params.code);
  const payload = await request.json().catch(() => null);

  return forward(request, `/admin/crawler-blueprints/${encoded}/activate`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(payload ?? {}),
  });
}

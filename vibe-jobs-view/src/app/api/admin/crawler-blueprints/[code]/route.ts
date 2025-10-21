import { NextRequest } from 'next/server';

import { forward } from '../proxy';

export async function GET(request: NextRequest, { params }: { params: { code: string } }) {
  const encoded = encodeURIComponent(params.code);
  return forward(request, `/admin/crawler-blueprints/${encoded}`);
}

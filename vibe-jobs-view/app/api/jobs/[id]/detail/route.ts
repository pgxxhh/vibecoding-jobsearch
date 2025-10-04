import { NextRequest, NextResponse } from 'next/server';
import { buildBackendUrl, resolveBackendBase } from '@/lib/backend';

type Params = { params: { id: string } };

export async function GET(req: NextRequest, { params }: Params) {
  const { id } = params;
  const base = resolveBackendBase(req);

  if (!base) {
    return NextResponse.json({ error: 'Backend base URL not configured' }, { status: 500 });
  }

  const upstream = buildBackendUrl(base, `/jobs/${id}/detail`);
  const res = await fetch(upstream, { headers: { accept: 'application/json' } });
  const text = await res.text();

  try {
    const json = JSON.parse(text);
    const content = json.content ?? json.description ?? '';
    return NextResponse.json(
      {
        id: String(json.id ?? id),
        title: json.title ?? '',
        company: json.company ?? '',
        location: json.location ?? '',
        postedAt: json.postedAt ?? '',
        content,
      },
      { status: res.status },
    );
  } catch {
    return NextResponse.json({ error: 'Invalid JSON from backend', raw: text }, { status: 502 });
  }
}

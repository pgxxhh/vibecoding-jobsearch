import { NextRequest, NextResponse } from 'next/server';

import { MOCK_JOBS } from '../../mock-data';

type Params = { params: { id: string } };

function buildBackendUrl(base: string, path: string) {
  const url = new URL(base);
  const basePath = url.pathname.replace(/\/$/, '');
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  const prefix = basePath.endsWith('/api') ? basePath : `${basePath}/api`;
  url.pathname = `${prefix}${normalizedPath}`;
  return url;
}

export async function GET(_req: NextRequest, { params }: Params) {
  const { id } = params;
  const base = process.env.BACKEND_BASE_URL;

  if (base) {
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
          description: content,
        },
        { status: res.status },
      );
    } catch {
      return NextResponse.json({ error: 'Invalid JSON from backend', raw: text }, { status: 502 });
    }
  }

  const job = MOCK_JOBS.find((item) => item.id === id);
  if (!job) {
    return NextResponse.json({ error: 'Job not found' }, { status: 404 });
  }

  const content = job.content ?? job.description ?? '';
  return NextResponse.json({
    id: job.id,
    title: job.title,
    company: job.company,
    location: job.location,
    postedAt: job.postedAt,
    content,
    description: content,
  });
}

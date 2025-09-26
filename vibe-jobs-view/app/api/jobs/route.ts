
import { NextRequest, NextResponse } from 'next/server';

import { MOCK_JOBS } from './mock-data';

export async function GET(req: NextRequest) {
  const url = new URL(req.url);
  const q = url.searchParams.get('q')?.toLowerCase() ?? '';
  const company = url.searchParams.get('company')?.toLowerCase() ?? '';
  const location = url.searchParams.get('location')?.toLowerCase() ?? '';
  const level = url.searchParams.get('level')?.toLowerCase() ?? '';
  const page = Number(url.searchParams.get('page') ?? '1');
  const size = Number(url.searchParams.get('size') ?? '10');

  const base = process.env.BACKEND_BASE_URL;
  if (base) {
    const upstream = new URL('/jobs', base);
    for (const [k, v] of url.searchParams.entries()) upstream.searchParams.set(k, v);
    const r = await fetch(upstream, { headers: { 'accept': 'application/json' } });
    const text = await r.text();
    try {
      const json = JSON.parse(text);
      return NextResponse.json(json, { status: r.status });
    } catch {
      return NextResponse.json({ error: 'Invalid JSON from backend', raw: text }, { status: 502 });
    }
  }

  const filtered = MOCK_JOBS.filter(j =>
    (!q || [j.title, (j.tags ?? []).join(' ')].join(' ').toLowerCase().includes(q)) &&
    (!company || j.company.toLowerCase().includes(company)) &&
    (!location || j.location.toLowerCase().includes(location)) &&
    (!level || (j.level ?? '').toLowerCase() === level)
  );
  const start = (page - 1) * size;
  const items = filtered.slice(start, start + size);
  return NextResponse.json({ items, total: filtered.length, page, size });
}

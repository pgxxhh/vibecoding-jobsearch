
import { NextRequest, NextResponse } from 'next/server';

const MOCK = [
  { id: '1', title: 'Senior Backend Engineer (Java)', company: 'Acme Global', location: 'Tokyo, JP', level: 'Senior',
    postedAt: new Date(Date.now() - 1000*60*60*24*2).toISOString(), tags: ['Java', 'Spring Boot', 'Microservices'], url: 'https://careers.example.com/jobs/1' },
  { id: '2', title: 'Staff Software Engineer - Payments', company: 'Globex', location: 'Singapore, SG', level: 'Staff',
    postedAt: new Date(Date.now() - 1000*60*60*24*7).toISOString(), tags: ['Distributed Systems', 'Kubernetes', 'MySQL'], url: 'https://careers.example.com/jobs/2' },
  { id: '3', title: 'Senior Platform Engineer', company: 'Initech', location: 'Shanghai, CN', level: 'Senior',
    postedAt: new Date(Date.now() - 1000*60*60*24*12).toISOString(), tags: ['Infra', 'Observability', 'SRE'], url: 'https://careers.example.com/jobs/3' }
];

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

  const filtered = MOCK.filter(j =>
    (!q || [j.title, (j.tags ?? []).join(' ')].join(' ').toLowerCase().includes(q)) &&
    (!company || j.company.toLowerCase().includes(company)) &&
    (!location || j.location.toLowerCase().includes(location)) &&
    (!level || (j.level ?? '').toLowerCase() === level)
  );
  const start = (page - 1) * size;
  const items = filtered.slice(start, start + size);
  return NextResponse.json({ items, total: filtered.length, page, size });
}


# Elaine Jobs — Minimal Template (Next.js + Tailwind + React Query)

A tiny starter for a jobs site: filters + list + pagination, with an API route
that either proxies to your Java backend or serves mock data.

## Stack
- Next.js (App Router) + TypeScript
- Tailwind CSS
- @tanstack/react-query v5

## Run locally
```bash
pnpm i   # or npm i / yarn
pnpm dev # http://localhost:3000
```

## Connect to your Java backend
Set `BACKEND_BASE_URL` so `/api/jobs` will proxy to your backend's `/api/jobs` endpoint:
```bash
BACKEND_BASE_URL="http://localhost:8080" pnpm dev
```
When running inside Docker (or any environment where the Java service is available as `backend:8080`),
set `BACKEND_BASE_URL="http://backend:8080"` so the Next.js API routes can call it directly over the
container network.
Your `/api/jobs` endpoint should accept query params like: `q, company, location, level, page, size` and return:
```json
{
  "items": [ { "id": "...", "title": "...", "company": "...", "location": "...", "level": "Senior", "postedAt": "ISO", "tags": ["Java"], "url": "..." } ],
  "total": 123,
  "page": 1,
  "size": 10
}
```

## Customize
- Edit `app/page.tsx` to tweak filters and list UX.
- Replace the mock in `app/api/jobs/route.ts` with your fields or leave proxy mode.
- Add auth / favorites / SEO pages as needed.

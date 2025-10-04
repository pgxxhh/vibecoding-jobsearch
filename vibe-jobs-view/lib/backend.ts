import type { NextRequest } from 'next/server';
import { headers as nextHeaders } from 'next/headers';

type RequestLike = NextRequest | Request;

function firstHeaderValue(value: string | null): string | null {
  if (!value) return null;
  const first = value.split(',')[0]?.trim();
  return first && first.length > 0 ? first : null;
}

function resolveOrigin(request?: RequestLike): string | null {
  if (request) {
    const maybeNextRequest = request as Partial<NextRequest>;
    const nextUrl = maybeNextRequest.nextUrl as URL | undefined;
    if (nextUrl?.origin) {
      return nextUrl.origin;
    }

    const requestHeaders = request.headers;
    const host = requestHeaders.get('x-forwarded-host') || requestHeaders.get('host');
    if (host) {
      const proto =
        firstHeaderValue(requestHeaders.get('x-forwarded-proto')) ||
        firstHeaderValue(requestHeaders.get('protocol')) ||
        (host.includes('localhost') || host.startsWith('127.') ? 'http' : null) ||
        'https';
      return `${proto}://${host}`;
    }
  }

  try {
    const headerList = nextHeaders();
    const host = headerList.get('x-forwarded-host') || headerList.get('host');
    if (!host) {
      return null;
    }
    const proto =
      firstHeaderValue(headerList.get('x-forwarded-proto')) ||
      (host.includes('localhost') || host.startsWith('127.') ? 'http' : 'https');
    return `${proto}://${host}`;
  } catch {
    return null;
  }
}

function normalizeProtocolRelativeUrl(url: string, origin: string | null): string {
  if (!url.startsWith('//')) {
    return url;
  }
  const effectiveOrigin = origin || 'https://localhost';
  const protocol = effectiveOrigin.split('://')[0] || 'https';
  return `${protocol}:${url}`;
}

export function resolveBackendBase(request?: RequestLike): string | null {
  const runtimeBase = process.env['BACKEND_BASE_URL'];
  if (runtimeBase && runtimeBase.trim()) {
    return runtimeBase.trim();
  }

  const origin = resolveOrigin(request);

  const candidates = [process.env['NEXT_PUBLIC_BACKEND_BASE'], process.env['NEXT_PUBLIC_API_BASE']];

  for (const candidate of candidates) {
    if (!candidate) continue;
    const trimmed = candidate.trim();
    if (!trimmed) continue;

    if (/^https?:\/\//.test(trimmed)) {
      return trimmed;
    }

    if (/^\/\//.test(trimmed)) {
      return normalizeProtocolRelativeUrl(trimmed, origin);
    }

    if (trimmed.startsWith('/')) {
      if (origin) {
        return `${origin}${trimmed}`;
      }
      continue;
    }
  }

  return null;
}

export function buildBackendUrl(base: string, path: string): URL {
  const url = new URL(base);
  const basePath = url.pathname.replace(/\/$/, '');
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  const prefix = basePath.endsWith('/api') ? basePath : `${basePath}/api`;
  url.pathname = `${prefix}${normalizedPath}`;
  return url;
}

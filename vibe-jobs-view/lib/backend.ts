export function resolveBackendBase(): string | null {
  const runtimeBase = process.env['BACKEND_BASE_URL'];
  if (runtimeBase && runtimeBase.trim()) {
    return runtimeBase.trim();
  }

  const candidates = [process.env['NEXT_PUBLIC_BACKEND_BASE'], process.env['NEXT_PUBLIC_API_BASE']];

  for (const candidate of candidates) {
    if (!candidate) continue;
    const trimmed = candidate.trim();
    if (trimmed && /^(https?:)?\/\//.test(trimmed)) {
      return trimmed;
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

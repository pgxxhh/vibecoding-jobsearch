function hasApiSegment(pathname: string): boolean {
  return /\/api(\/|$)/.test(pathname);
}

function ensureTrailingApi(pathname: string): string {
  const trimmed = pathname.replace(/\/+$/, '');
  if (!trimmed) {
    return '/api';
  }
  if (hasApiSegment(trimmed)) {
    return trimmed;
  }
  return `${trimmed}/api`;
}

function normalizeAbsoluteBase(raw: string): string {
  try {
    const url = new URL(raw);
    url.pathname = ensureTrailingApi(url.pathname || '/');
    return url.toString().replace(/\/+$/, '');
  } catch {
    return raw;
  }
}

function normalizeProtocolRelativeBase(raw: string): string {
  try {
    const url = new URL(`http:${raw}`);
    url.pathname = ensureTrailingApi(url.pathname || '/');
    const normalized = url.toString().replace(/^http:/, '');
    return normalized.replace(/\/+$/, '');
  } catch {
    return raw;
  }
}

function normalizeRelativeBase(raw: string): string {
  const trimmed = raw.trim();
  if (!trimmed) {
    return '/api';
  }
  const withoutTrailing = trimmed.replace(/\/+$/, '');
  if (!withoutTrailing) {
    return '/api';
  }
  if (hasApiSegment(withoutTrailing)) {
    return withoutTrailing;
  }
  return `${withoutTrailing}/api`;
}

function ensureApiBase(raw: string): string {
  const trimmed = raw.trim();
  if (!trimmed) {
    return '/api';
  }
  if (/^https?:\/\//.test(trimmed)) {
    return normalizeAbsoluteBase(trimmed);
  }
  if (trimmed.startsWith('//')) {
    return normalizeProtocolRelativeBase(trimmed);
  }
  return normalizeRelativeBase(trimmed);
}

export function resolvePublicApiBase(): string {
  const backendBase = process.env.NEXT_PUBLIC_BACKEND_BASE?.trim();
  if (backendBase) {
    return ensureApiBase(backendBase);
  }

  const apiBase = process.env.NEXT_PUBLIC_API_BASE?.trim();
  if (apiBase) {
    return apiBase.replace(/\/+$/, '') || '/api';
  }

  return '/api';
}

export function joinApiPath(path: string): string {
  const base = resolvePublicApiBase();
  if (path.startsWith('/')) {
    return `${base}${path}`;
  }
  return `${base}/${path}`;
}

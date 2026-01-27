import type { UseMutateFunction } from '@tanstack/react-query';

import type { CompanyDiscoverySettings } from '@/modules/admin/types';

export interface CompanyDiscoveryFormState {
  enabled: boolean;
  fixedDelayMs: string;
  initialDelayMs: string;
  pageSize: string;
  maxCandidatesPerRun: string;
  dryRun: boolean;
  includeDataSourceTypes: string;
  excludeCompanies: string;
  providersJson: string;
  locationJson: string;
  roleJson: string;
}

function ensureNumber(value: string, fallback?: number): number {
  const parsed = Number(value);
  if (Number.isFinite(parsed) && parsed >= 0) {
    return parsed;
  }
  if (fallback !== undefined) {
    return fallback;
  }
  throw new Error('数值字段不能为空');
}

function parseJson(label: string, raw: string, fallback: Record<string, unknown> | unknown[]): any {
  const trimmed = raw.trim();
  if (!trimmed) {
    return fallback;
  }
  try {
    return JSON.parse(trimmed);
  } catch {
    throw new Error(`${label} JSON 解析失败`);
  }
}

function parseList(raw: string, fallback: string[] = []) {
  const trimmed = raw.trim();
  if (!trimmed) {
    return fallback;
  }
  return trimmed
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

export function buildCompanyDiscoverySettingsPayload(
  form: CompanyDiscoveryFormState,
  fallback?: CompanyDiscoverySettings | null,
): Partial<CompanyDiscoverySettings> {
  const {
    enabled,
    fixedDelayMs,
    initialDelayMs,
    pageSize,
    maxCandidatesPerRun,
    dryRun,
    includeDataSourceTypes,
    excludeCompanies,
    providersJson,
    locationJson,
    roleJson,
  } = form;

  if (!fixedDelayMs || !initialDelayMs || !pageSize || !maxCandidatesPerRun) {
    throw new Error('请填写所有必填字段');
  }

  const providers = parseJson('Providers', providersJson, fallback?.providers ?? {});
  const location = parseJson('Location', locationJson, fallback?.locationFilter ?? {});
  const role = parseJson('Role', roleJson, fallback?.roleFilter ?? {});

  return {
    enabled,
    fixedDelayMs: ensureNumber(fixedDelayMs, fallback?.fixedDelayMs),
    initialDelayMs: ensureNumber(initialDelayMs, fallback?.initialDelayMs),
    pageSize: ensureNumber(pageSize, fallback?.pageSize),
    maxCandidatesPerRun: ensureNumber(maxCandidatesPerRun, fallback?.maxCandidatesPerRun),
    dryRun,
    includeDataSourceTypes: parseList(includeDataSourceTypes, fallback?.includeDataSourceTypes ?? []),
    excludeCompanies: parseList(excludeCompanies, fallback?.excludeCompanies ?? []),
    providers,
    locationFilter: location,
    roleFilter: role,
  };
}

type UpdateMutateFn = UseMutateFunction<
  CompanyDiscoverySettings,
  Error,
  Partial<CompanyDiscoverySettings>,
  unknown
>;

export function submitCompanyDiscoverySettingsForm(
  form: CompanyDiscoveryFormState,
  options: {
    fallback?: CompanyDiscoverySettings | null;
    mutate: UpdateMutateFn;
    onSuccess: () => void;
    onError: (err: unknown) => void;
  },
) {
  const payload = buildCompanyDiscoverySettingsPayload(form, options.fallback);
  options.mutate(payload, {
    onSuccess: options.onSuccess,
    onError: options.onError,
  });
}

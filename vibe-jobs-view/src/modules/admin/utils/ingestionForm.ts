import type { UseMutateFunction } from '@tanstack/react-query';

import type { IngestionSettings } from '@/modules/admin/types';

export interface IngestionFormState {
  fixedDelayMs: string;
  initialDelayMs: string;
  pageSize: string;
  recentDays: string;
  concurrency: string;
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

export function buildIngestionSettingsPayload(
  form: IngestionFormState,
  fallback?: IngestionSettings | null,
): Partial<IngestionSettings> {
  const { fixedDelayMs, initialDelayMs, pageSize, recentDays, concurrency, locationJson, roleJson } = form;

  if (!fixedDelayMs || !initialDelayMs || !pageSize || !recentDays || !concurrency) {
    throw new Error('请填写所有必填字段');
  }

  const location = parseJson('Location', locationJson, {});
  const role = parseJson('Role', roleJson, {});

  return {
    fixedDelayMs: ensureNumber(fixedDelayMs, fallback?.fixedDelayMs),
    initialDelayMs: ensureNumber(initialDelayMs, fallback?.initialDelayMs),
    pageSize: ensureNumber(pageSize, fallback?.pageSize),
    recentDays: ensureNumber(recentDays, fallback?.recentDays),
    concurrency: ensureNumber(concurrency, fallback?.concurrency),
    companyOverrides: fallback?.companyOverrides ?? {},
    locationFilter: location,
    roleFilter: role,
  };
}

type UpdateMutateFn = UseMutateFunction<
  IngestionSettings,
  Error,
  Partial<IngestionSettings>,
  unknown
>;

export function submitIngestionSettingsForm(
  form: IngestionFormState,
  options: {
    fallback?: IngestionSettings | null;
    mutate: UpdateMutateFn;
    onSuccess: () => void;
    onError: (err: unknown) => void;
  },
) {
  const payload = buildIngestionSettingsPayload(form, options.fallback);
  options.mutate(payload, {
    onSuccess: options.onSuccess,
    onError: options.onError,
  });
}

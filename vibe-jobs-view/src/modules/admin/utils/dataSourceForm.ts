import type { UseMutateFunction } from '@tanstack/react-query';

import type { DataSourcePayload, DataSourceResponse } from '@/modules/admin/types';

export interface DataSourceFormState {
  code: string;
  type: string;
  enabled: boolean;
  runOnStartup: boolean;
  requireOverride: boolean;
  flow: 'LIMITED' | 'UNLIMITED';
  baseOptionsJson: string;
  categoriesJson: string;
  companiesJson: string;
}

function parseJson<T>(label: string, raw: string, fallback: T): T {
  const trimmed = raw.trim();
  if (!trimmed) {
    return fallback;
  }

  try {
    return JSON.parse(trimmed) as T;
  } catch (error) {
    throw new Error(`${label} JSON 解析失败`);
  }
}

export function buildDataSourcePayload(form: DataSourceFormState): DataSourcePayload {
  if (!form.code) {
    throw new Error('Code 不能为空');
  }
  if (!form.type) {
    throw new Error('Type 不能为空');
  }

  const baseOptions = parseJson<DataSourcePayload['baseOptions']>('BaseOptions', form.baseOptionsJson, {});
  const categories = parseJson<DataSourcePayload['categories']>('Categories', form.categoriesJson, []);
  const companies = parseJson<DataSourcePayload['companies']>('Companies', form.companiesJson, []);

  return {
    code: form.code,
    type: form.type,
    enabled: form.enabled,
    runOnStartup: form.runOnStartup,
    requireOverride: form.requireOverride,
    flow: form.flow,
    baseOptions,
    categories,
    companies,
  };
}

type SaveMutateFn = UseMutateFunction<
  DataSourceResponse,
  Error,
  { payload: DataSourcePayload; id?: number | 'new' | null },
  unknown
>;

type RemoveMutateFn = UseMutateFunction<unknown, Error, number, unknown>;

export function submitDataSourceForm(
  form: DataSourceFormState,
  options: {
    id: number | 'new' | null;
    mutate: SaveMutateFn;
    onSuccess: () => void;
    onError: (err: unknown) => void;
  },
) {
  const payload = buildDataSourcePayload(form);
  options.mutate(
    { payload, id: options.id },
    {
      onSuccess: options.onSuccess,
      onError: options.onError,
    },
  );
}

export function removeDataSourceById(
  id: number,
  options: {
    mutate: RemoveMutateFn;
    onSuccess: () => void;
    onError: (err: unknown) => void;
  },
) {
  options.mutate(id, {
    onSuccess: options.onSuccess,
    onError: options.onError,
  });
}

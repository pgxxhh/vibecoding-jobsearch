'use client';

import { FormEvent, useEffect, useState } from 'react';

import { useCompanyDiscoverySettings } from '@/modules/admin/hooks/useCompanyDiscoverySettings';
import { useCompanyDiscoveryRuns } from '@/modules/admin/hooks/useCompanyDiscoveryRuns';
import { useCompanyDiscoveryResults } from '@/modules/admin/hooks/useCompanyDiscoveryResults';
import {
  submitCompanyDiscoverySettingsForm,
  type CompanyDiscoveryFormState,
} from '@/modules/admin/utils/companyDiscoveryForm';
import type { CompanyDiscoveryRun, CompanyDiscoveryResult } from '@/modules/admin/types';

function formatTimestamp(value?: string | null) {
  if (!value) return '-';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '-' : date.toLocaleString('zh-CN');
}

export default function CompanyDiscoveryPage() {
  const { query, update } = useCompanyDiscoverySettings();
  const runs = useCompanyDiscoveryRuns();
  const results = useCompanyDiscoveryResults();

  const { data, isLoading, isError, error } = query;

  const [enabled, setEnabled] = useState(false);
  const [fixedDelayMs, setFixedDelayMs] = useState('86400000');
  const [initialDelayMs, setInitialDelayMs] = useState('10000');
  const [pageSize, setPageSize] = useState('50');
  const [maxCandidatesPerRun, setMaxCandidatesPerRun] = useState('200');
  const [dryRun, setDryRun] = useState(true);
  const [includeTypes, setIncludeTypes] = useState('');
  const [excludeCompanies, setExcludeCompanies] = useState('');
  const [providersJson, setProvidersJson] = useState('');
  const [locationJson, setLocationJson] = useState('');
  const [roleJson, setRoleJson] = useState('');
  const [message, setMessage] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useEffect(() => {
    if (data) {
      setEnabled(Boolean(data.enabled));
      setFixedDelayMs(String(data.fixedDelayMs));
      setInitialDelayMs(String(data.initialDelayMs));
      setPageSize(String(data.pageSize));
      setMaxCandidatesPerRun(String(data.maxCandidatesPerRun));
      setDryRun(Boolean(data.dryRun));
      setIncludeTypes(data.includeDataSourceTypes?.join(', ') ?? '');
      setExcludeCompanies(data.excludeCompanies?.join(', ') ?? '');
      setProvidersJson(JSON.stringify(data.providers ?? {}, null, 2));
      setLocationJson(JSON.stringify(data.locationFilter ?? {}, null, 2));
      setRoleJson(JSON.stringify(data.roleFilter ?? {}, null, 2));
    }
  }, [data]);

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formState: CompanyDiscoveryFormState = {
      enabled,
      fixedDelayMs,
      initialDelayMs,
      pageSize,
      maxCandidatesPerRun,
      dryRun,
      includeDataSourceTypes: includeTypes,
      excludeCompanies,
      providersJson,
      locationJson,
      roleJson,
    };

    try {
      submitCompanyDiscoverySettingsForm(formState, {
        fallback: data,
        mutate: update.mutate,
        onSuccess: () => {
          setMessage('配置已保存，后台任务将在 1-2 秒内重新调度');
          setErrorMsg(null);
        },
        onError: (err: unknown) => {
          setMessage(null);
          setErrorMsg(err instanceof Error ? err.message : '保存失败');
        },
      });
    } catch (err) {
      setMessage(null);
      setErrorMsg(err instanceof Error ? err.message : '无法解析 JSON 配置');
    }
  };

  const handleTrigger = () => {
    runs.trigger.mutate(undefined, {
      onSuccess: () => {
        setMessage('已触发一次公司发现任务，请稍后刷新查看结果');
        setErrorMsg(null);
      },
      onError: (err: unknown) => {
        setMessage(null);
        setErrorMsg(err instanceof Error ? err.message : '触发失败');
      },
    });
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="text-center">
          <div className="mx-auto h-8 w-8 animate-spin rounded-full border-4 border-brand-200 border-t-brand-600"></div>
          <p className="mt-4 text-lg font-semibold text-gray-900">加载配置中...</p>
        </div>
      </div>
    );
  }

  if (isError || !data) {
    return (
      <div className="rounded-xl bg-rose-50 border border-rose-200 p-6">
        <p className="text-rose-800">{(error as Error)?.message ?? '加载失败'}</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h2 className="text-2xl font-semibold text-gray-900">公司发现配置</h2>
          <p className="text-sm text-gray-600 mt-1">用于自动发现并验证新公司，保存后调度实时生效。</p>
          <div className="mt-2 text-xs text-gray-500">最后更新: {formatTimestamp(data.updatedAt)}</div>
        </div>
        <button
          onClick={handleTrigger}
          className="inline-flex items-center justify-center gap-2 rounded-2xl transition active:scale-[.98] h-10 px-4 text-sm bg-brand-600 text-white hover:bg-brand-700 shadow-brand-sm focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-500/30"
          disabled={runs.trigger.isPending}
        >
          触发一次发现
        </button>
      </div>

      {message && (
        <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
          {message}
        </div>
      )}
      {errorMsg && (
        <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {errorMsg}
        </div>
      )}

      <form className="space-y-6" onSubmit={handleSubmit}>
        <div className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-semibold text-gray-900">🧭 全局开关</h3>
            <label className="inline-flex items-center gap-2 text-sm text-gray-700">
              <input
                type="checkbox"
                checked={enabled}
                onChange={(e) => setEnabled(e.target.checked)}
                className="h-4 w-4 rounded border-gray-300 text-brand-600 focus:ring-brand-500"
              />
              启用公司发现
            </label>
          </div>
          <label className="inline-flex items-center gap-2 text-sm text-gray-700">
            <input
              type="checkbox"
              checked={dryRun}
              onChange={(e) => setDryRun(e.target.checked)}
              className="h-4 w-4 rounded border-gray-300 text-brand-600 focus:ring-brand-500"
            />
            Dry-run（只验证不入库）
          </label>
        </div>

        <div className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">⏱️ 调度配置</h3>
          <div className="grid gap-4 md:grid-cols-2">
            <label className="flex flex-col space-y-2 text-sm">
              <span className="font-medium text-gray-700">固定延迟（毫秒）*</span>
              <input
                value={fixedDelayMs}
                onChange={(e) => setFixedDelayMs(e.target.value)}
                className="rounded-xl border border-gray-200 bg-white px-4 py-3 text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
                type="number"
                min={1000}
                step={1000}
                required
              />
            </label>
            <label className="flex flex-col space-y-2 text-sm">
              <span className="font-medium text-gray-700">初始延迟（毫秒）*</span>
              <input
                value={initialDelayMs}
                onChange={(e) => setInitialDelayMs(e.target.value)}
                className="rounded-xl border border-gray-200 bg-white px-4 py-3 text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
                type="number"
                min={0}
                step={1000}
                required
              />
            </label>
            <label className="flex flex-col space-y-2 text-sm">
              <span className="font-medium text-gray-700">校验页大小*</span>
              <input
                value={pageSize}
                onChange={(e) => setPageSize(e.target.value)}
                className="rounded-xl border border-gray-200 bg-white px-4 py-3 text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
                type="number"
                min={1}
                max={200}
                required
              />
            </label>
            <label className="flex flex-col space-y-2 text-sm">
              <span className="font-medium text-gray-700">单次最大候选数*</span>
              <input
                value={maxCandidatesPerRun}
                onChange={(e) => setMaxCandidatesPerRun(e.target.value)}
                className="rounded-xl border border-gray-200 bg-white px-4 py-3 text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
                type="number"
                min={1}
                max={1000}
                required
              />
            </label>
          </div>
        </div>

        <div className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">🏷️ 数据源范围</h3>
          <div className="grid gap-4 md:grid-cols-2">
            <label className="flex flex-col space-y-2 text-sm">
              <span className="font-medium text-gray-700">允许的 data source type</span>
              <input
                value={includeTypes}
                onChange={(e) => setIncludeTypes(e.target.value)}
                className="rounded-xl border border-gray-200 bg-white px-4 py-3 text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
                placeholder="greenhouse, lever, smartrecruiters"
              />
            </label>
            <label className="flex flex-col space-y-2 text-sm">
              <span className="font-medium text-gray-700">排除公司（reference）</span>
              <input
                value={excludeCompanies}
                onChange={(e) => setExcludeCompanies(e.target.value)}
                className="rounded-xl border border-gray-200 bg-white px-4 py-3 text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
                placeholder="example-inc, bad-co"
              />
            </label>
          </div>
        </div>

        <div className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">🧩 Provider 配置</h3>
          <label className="flex flex-col space-y-2 text-sm">
            <span className="font-medium text-gray-700">Provider JSON</span>
            <textarea
              value={providersJson}
              onChange={(e) => setProvidersJson(e.target.value)}
              rows={8}
              className="rounded-xl border border-gray-200 bg-white px-4 py-3 font-mono text-xs text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
              placeholder={`{\n  "smartrecruiters": {\n    "enabled": true,\n    "baseUrl": "https://api.smartrecruiters.com/v1/companies"\n  },\n  "seed": {\n    "enabled": true,\n    "seedCompanies": ["stripe", "airbnb"]\n  }\n}`}
            />
          </label>
        </div>

        <div className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">🔍 过滤器配置</h3>
          <div className="grid gap-4 md:grid-cols-2">
            <label className="flex flex-col space-y-2 text-sm">
              <span className="font-medium text-gray-700">地点过滤器（JSON）</span>
              <textarea
                value={locationJson}
                onChange={(e) => setLocationJson(e.target.value)}
                rows={8}
                className="rounded-xl border border-gray-200 bg-white px-4 py-3 font-mono text-xs text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
              />
            </label>
            <label className="flex flex-col space-y-2 text-sm">
              <span className="font-medium text-gray-700">职位过滤器（JSON）</span>
              <textarea
                value={roleJson}
                onChange={(e) => setRoleJson(e.target.value)}
                rows={8}
                className="rounded-xl border border-gray-200 bg-white px-4 py-3 font-mono text-xs text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
              />
            </label>
          </div>
        </div>

        <div className="flex justify-end">
          <button
            type="submit"
            className="inline-flex items-center justify-center gap-2 rounded-2xl transition active:scale-[.98] h-11 px-6 text-sm bg-brand-600 text-white hover:bg-brand-700 shadow-brand-sm focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-500/30"
            disabled={update.isPending}
          >
            保存配置
          </button>
        </div>
      </form>

      <section className="space-y-6">
        <div>
          <h3 className="text-lg font-semibold text-gray-900">最近运行记录</h3>
          <p className="text-sm text-gray-600">展示最近 50 次运行的概览。</p>
        </div>
        <RunsTable runs={runs.query.data ?? []} loading={runs.query.isLoading} />
      </section>

      <section className="space-y-6">
        <div>
          <h3 className="text-lg font-semibold text-gray-900">新增公司列表</h3>
          <p className="text-sm text-gray-600">展示最近 100 条公司发现记录（含验证结果）。</p>
        </div>
        <ResultsTable results={results.data ?? []} loading={results.isLoading} />
      </section>
    </div>
  );
}

function RunsTable({ runs, loading }: { runs: CompanyDiscoveryRun[]; loading: boolean }) {
  if (loading) {
    return <p className="text-sm text-gray-500">加载中...</p>;
  }

  if (runs.length === 0) {
    return <p className="text-sm text-gray-500">暂无运行记录</p>;
  }

  return (
    <div className="overflow-hidden rounded-2xl border border-gray-200 bg-white">
      <table className="min-w-full divide-y divide-gray-200 text-sm">
        <thead className="bg-gray-50 text-gray-600">
          <tr>
            <th className="px-4 py-3 text-left font-medium">运行时间</th>
            <th className="px-4 py-3 text-left font-medium">状态</th>
            <th className="px-4 py-3 text-left font-medium">Dry-run</th>
            <th className="px-4 py-3 text-left font-medium">候选数</th>
            <th className="px-4 py-3 text-left font-medium">有效数</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100 text-gray-900">
          {runs.map((run) => (
            <tr key={`${run.id}-${run.startedAt}`}>
              <td className="px-4 py-3">{formatTimestamp(run.startedAt ?? run.completedAt ?? undefined)}</td>
              <td className="px-4 py-3">{run.status}</td>
              <td className="px-4 py-3">{run.dryRun ? '是' : '否'}</td>
              <td className="px-4 py-3">{run.totalCandidates}</td>
              <td className="px-4 py-3">{run.totalValid}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function ResultsTable({ results, loading }: { results: CompanyDiscoveryResult[]; loading: boolean }) {
  if (loading) {
    return <p className="text-sm text-gray-500">加载中...</p>;
  }

  if (results.length === 0) {
    return <p className="text-sm text-gray-500">暂无公司记录</p>;
  }

  return (
    <div className="overflow-hidden rounded-2xl border border-gray-200 bg-white">
      <table className="min-w-full divide-y divide-gray-200 text-sm">
        <thead className="bg-gray-50 text-gray-600">
          <tr>
            <th className="px-4 py-3 text-left font-medium">时间</th>
            <th className="px-4 py-3 text-left font-medium">数据源</th>
            <th className="px-4 py-3 text-left font-medium">公司</th>
            <th className="px-4 py-3 text-left font-medium">来源</th>
            <th className="px-4 py-3 text-left font-medium">状态</th>
            <th className="px-4 py-3 text-left font-medium">原因</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100 text-gray-900">
          {results.map((result) => (
            <tr key={result.id}>
              <td className="px-4 py-3">{formatTimestamp(result.createdAt)}</td>
              <td className="px-4 py-3">{result.dataSourceCode}</td>
              <td className="px-4 py-3">
                <div className="font-medium">{result.displayName || result.companyReference}</div>
                {result.displayName && (
                  <div className="text-xs text-gray-500">{result.companyReference}</div>
                )}
              </td>
              <td className="px-4 py-3">{result.provider ?? '-'}</td>
              <td className="px-4 py-3">{result.status}</td>
              <td className="px-4 py-3 text-xs text-gray-500">{result.reason ?? '-'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

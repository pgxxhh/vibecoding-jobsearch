'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FormEvent, useEffect, useMemo, useState } from 'react';

interface CategoryQuotaDefinition {
  name: string;
  limit: number;
  tags: string[];
  facets: Record<string, string[]>;
}

interface DataSourceCompany {
  id: number | null;
  reference: string;
  displayName: string;
  slug: string;
  enabled: boolean;
  placeholderOverrides: Record<string, string>;
  overrideOptions: Record<string, string>;
}

interface DataSourceResponse {
  id: number;
  code: string;
  type: string;
  enabled: boolean;
  runOnStartup: boolean;
  requireOverride: boolean;
  flow: 'LIMITED' | 'UNLIMITED';
  baseOptions: Record<string, string>;
  categories: CategoryQuotaDefinition[];
  companies: DataSourceCompany[];
}

async function fetchDataSources(): Promise<DataSourceResponse[]> {
  const res = await fetch('/api/admin/data-sources', { cache: 'no-store' });
  if (!res.ok) {
    throw new Error('无法获取数据源列表');
  }
  return res.json();
}

export default function DataSourcesPage() {
  const queryClient = useQueryClient();
  const { data, isLoading, isError, error } = useQuery({ queryKey: ['admin', 'data-sources'], queryFn: fetchDataSources });
  const [selectedId, setSelectedId] = useState<number | 'new' | null>(null);
  const [form, setForm] = useState({
    code: '',
    type: '',
    enabled: true,
    runOnStartup: false,
    requireOverride: false,
    flow: 'UNLIMITED' as 'LIMITED' | 'UNLIMITED',
    baseOptionsJson: '{}',
    categoriesJson: '[]',
    companiesJson: '[]',
  });
  const [message, setMessage] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const selectedSource = useMemo(() => {
    if (!data) return null;
    if (selectedId === null || selectedId === 'new') return null;
    return data.find((item) => item.id === selectedId) ?? null;
  }, [data, selectedId]);

  useEffect(() => {
    if (selectedSource) {
      setForm({
        code: selectedSource.code,
        type: selectedSource.type,
        enabled: selectedSource.enabled,
        runOnStartup: selectedSource.runOnStartup,
        requireOverride: selectedSource.requireOverride,
        flow: selectedSource.flow,
        baseOptionsJson: JSON.stringify(selectedSource.baseOptions ?? {}, null, 2),
        categoriesJson: JSON.stringify(selectedSource.categories ?? [], null, 2),
        companiesJson: JSON.stringify(selectedSource.companies ?? [], null, 2),
      });
      setMessage(null);
      setErrorMsg(null);
    } else if (selectedId === 'new') {
      setForm({
        code: '',
        type: '',
        enabled: true,
        runOnStartup: false,
        requireOverride: false,
        flow: 'UNLIMITED',
        baseOptionsJson: '{}',
        categoriesJson: '[]',
        companiesJson: '[]',
      });
      setMessage(null);
      setErrorMsg(null);
    }
  }, [selectedId, selectedSource]);

  const saveMutation = useMutation({
    mutationFn: async (payload: Record<string, unknown>) => {
      const isNew = selectedId === 'new';
      const url = isNew ? '/api/admin/data-sources' : `/api/admin/data-sources/${selectedId}`;
      const method = isNew ? 'POST' : 'PUT';
      const res = await fetch(url, {
        method,
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify(payload),
      });
      if (!res.ok) {
        const text = await res.text();
        throw new Error(text || '保存失败');
      }
      return res.json();
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin', 'data-sources'] });
      setMessage('数据源已保存');
      setErrorMsg(null);
      setSelectedId(null);
    },
    onError: (err: unknown) => {
      setMessage(null);
      setErrorMsg(err instanceof Error ? err.message : '保存失败');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: number) => {
      const res = await fetch(`/api/admin/data-sources/${id}`, { method: 'DELETE' });
      if (!res.ok) {
        const text = await res.text();
        throw new Error(text || '删除失败');
      }
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin', 'data-sources'] });
      setSelectedId(null);
      setMessage('数据源已删除');
      setErrorMsg(null);
    },
    onError: (err: unknown) => {
      setMessage(null);
      setErrorMsg(err instanceof Error ? err.message : '删除失败');
    },
  });

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    try {
      const baseOptions = form.baseOptionsJson.trim() ? JSON.parse(form.baseOptionsJson) : {};
      const categories = form.categoriesJson.trim() ? JSON.parse(form.categoriesJson) : [];
      const companies = form.companiesJson.trim() ? JSON.parse(form.companiesJson) : [];
      if (!form.code) {
        throw new Error('Code 不能为空');
      }
      if (!form.type) {
        throw new Error('Type 不能为空');
      }
      const payload = {
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
      saveMutation.mutate(payload);
    } catch (err) {
      setMessage(null);
      setErrorMsg(err instanceof Error ? err.message : '无法解析 JSON');
    }
  };

  if (isLoading) {
    return <p className="text-white/80">加载中...</p>;
  }

  if (isError || !data) {
    return <p className="text-red-300">{(error as Error)?.message ?? '加载失败'}</p>;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-semibold text-white">数据源管理</h2>
          <p className="text-sm text-white/70">支持动态增删改，保存后缓存立即刷新。</p>
        </div>
        <button
          onClick={() => setSelectedId('new')}
          className="rounded-md bg-white/10 px-3 py-2 text-sm font-medium text-white hover:bg-white/20"
        >
          新建数据源
        </button>
      </div>

      <div className="grid gap-6 lg:grid-cols-[320px_1fr]">
        <aside className="space-y-3 rounded-xl border border-white/10 bg-white/5 p-4">
          {data.map((source) => (
            <button
              key={source.id}
              onClick={() => setSelectedId(source.id)}
              className={`w-full rounded-lg border px-3 py-2 text-left text-sm transition ${
                selectedId === source.id
                  ? 'border-white/40 bg-white/15 text-white'
                  : 'border-white/10 bg-transparent text-white/70 hover:border-white/30 hover:text-white'
              }`}
            >
              <div className="flex items-center justify-between">
                <span className="font-medium">{source.code}</span>
                <span className={`text-xs ${source.enabled ? 'text-emerald-300' : 'text-rose-300'}`}>
                  {source.enabled ? '启用' : '停用'}
                </span>
              </div>
              <p className="text-xs text-white/60">{source.type}</p>
            </button>
          ))}
          {data.length === 0 && <p className="text-sm text-white/60">暂无数据源</p>}
        </aside>

        <section className="rounded-xl border border-white/10 bg-white/5 p-4">
          {selectedId === null && <p className="text-sm text-white/70">请选择左侧数据源，或点击“新建数据源”。</p>}
          {selectedId !== null && (
            <form className="space-y-4" onSubmit={handleSubmit}>
              <div className="grid gap-4 md:grid-cols-2">
                <label className="flex flex-col space-y-1 text-sm text-white/80">
                  <span>Code</span>
                  <input
                    value={form.code}
                    onChange={(e) => setForm((prev) => ({ ...prev, code: e.target.value }))}
                    className="rounded-md border border-white/10 bg-white/5 px-3 py-2 text-white focus:border-white/40 focus:outline-none"
                    required
                  />
                </label>
                <label className="flex flex-col space-y-1 text-sm text-white/80">
                  <span>Type</span>
                  <input
                    value={form.type}
                    onChange={(e) => setForm((prev) => ({ ...prev, type: e.target.value }))}
                    className="rounded-md border border-white/10 bg-white/5 px-3 py-2 text-white focus:border-white/40 focus:outline-none"
                    required
                  />
                </label>
                <label className="flex items-center space-x-2 text-sm text-white/80">
                  <input
                    type="checkbox"
                    checked={form.enabled}
                    onChange={(e) => setForm((prev) => ({ ...prev, enabled: e.target.checked }))}
                    className="h-4 w-4 rounded border-white/20 bg-white/10"
                  />
                  <span>启用</span>
                </label>
                <label className="flex items-center space-x-2 text-sm text-white/80">
                  <input
                    type="checkbox"
                    checked={form.runOnStartup}
                    onChange={(e) => setForm((prev) => ({ ...prev, runOnStartup: e.target.checked }))}
                    className="h-4 w-4 rounded border-white/20 bg-white/10"
                  />
                  <span>启动时执行一次</span>
                </label>
                <label className="flex items-center space-x-2 text-sm text-white/80">
                  <input
                    type="checkbox"
                    checked={form.requireOverride}
                    onChange={(e) => setForm((prev) => ({ ...prev, requireOverride: e.target.checked }))}
                    className="h-4 w-4 rounded border-white/20 bg-white/10"
                  />
                  <span>需要公司 Override</span>
                </label>
                <label className="flex flex-col space-y-1 text-sm text-white/80">
                  <span>Flow</span>
                  <select
                    value={form.flow}
                    onChange={(e) => setForm((prev) => ({ ...prev, flow: e.target.value as 'LIMITED' | 'UNLIMITED' }))}
                    className="rounded-md border border-white/10 bg-white/5 px-3 py-2 text-white focus:border-white/40 focus:outline-none"
                  >
                    <option value="UNLIMITED">UNLIMITED</option>
                    <option value="LIMITED">LIMITED</option>
                  </select>
                </label>
              </div>

              <label className="flex flex-col space-y-1 text-sm text-white/80">
                <span>Base Options（JSON）</span>
                <textarea
                  value={form.baseOptionsJson}
                  onChange={(e) => setForm((prev) => ({ ...prev, baseOptionsJson: e.target.value }))}
                  rows={6}
                  className="rounded-md border border-white/10 bg-white/5 px-3 py-2 font-mono text-xs text-white focus:border-white/40 focus:outline-none"
                />
              </label>
              <label className="flex flex-col space-y-1 text-sm text-white/80">
                <span>Categories（JSON 数组）</span>
                <textarea
                  value={form.categoriesJson}
                  onChange={(e) => setForm((prev) => ({ ...prev, categoriesJson: e.target.value }))}
                  rows={6}
                  className="rounded-md border border-white/10 bg-white/5 px-3 py-2 font-mono text-xs text-white focus:border-white/40 focus:outline-none"
                />
              </label>
              <label className="flex flex-col space-y-1 text-sm text-white/80">
                <span>Companies（JSON 数组）</span>
                <textarea
                  value={form.companiesJson}
                  onChange={(e) => setForm((prev) => ({ ...prev, companiesJson: e.target.value }))}
                  rows={8}
                  className="rounded-md border border-white/10 bg-white/5 px-3 py-2 font-mono text-xs text-white focus:border-white/40 focus:outline-none"
                />
              </label>

              {message && <p className="text-sm text-emerald-300">{message}</p>}
              {errorMsg && <p className="text-sm text-rose-300">{errorMsg}</p>}

              <div className="flex items-center space-x-3">
                <button
                  type="submit"
                  disabled={saveMutation.isPending}
                  className="rounded-md bg-white/10 px-4 py-2 text-sm font-medium text-white hover:bg-white/20 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {saveMutation.isPending ? '保存中...' : '保存'}
                </button>
                {selectedId !== 'new' && selectedId !== null && (
                  <button
                    type="button"
                    onClick={() => selectedId && typeof selectedId === 'number' && deleteMutation.mutate(selectedId)}
                    className="rounded-md bg-rose-500/20 px-4 py-2 text-sm font-medium text-rose-200 hover:bg-rose-500/30"
                  >
                    删除
                  </button>
                )}
              </div>
            </form>
          )}
        </section>
      </div>
    </div>
  );
}

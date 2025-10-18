'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FormEvent, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import DataSourceBulkUpload from '@/modules/admin/components/DataSourceBulkUpload';

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
  const [showBulkUpload, setShowBulkUpload] = useState(false);

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
      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="text-2xl font-semibold text-gray-900">数据源管理</h2>
          <p className="text-sm text-gray-600 mt-1">支持动态增删改，保存后缓存立即刷新。</p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={() => setShowBulkUpload(true)}
            className="inline-flex items-center justify-center gap-2 rounded-2xl transition active:scale-[.98] h-10 px-4 text-sm bg-blue-600 text-white hover:bg-blue-700 shadow-brand-sm focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-blue-500/30"
          >
            📤 批量上传
          </button>
          <button
            onClick={() => setSelectedId('new')}
            className="inline-flex items-center justify-center gap-2 rounded-2xl transition active:scale-[.98] h-10 px-4 text-sm bg-brand-600 text-white hover:bg-brand-700 shadow-brand-sm focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-500/30"
          >
            ➕ 新建数据源
          </button>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[360px_1fr]">
        <aside className="space-y-3">
          <div className="rounded-2xl border border-gray-200 bg-white p-4 shadow-sm">
            <h3 className="mb-3 text-sm font-semibold text-gray-900">数据源列表</h3>
            <div className="space-y-2">
              {data.map((source) => (
                <div key={source.id} className="space-y-2">
                  <button
                    onClick={() => setSelectedId(source.id)}
                    className={`w-full rounded-xl border px-3 py-3 text-left text-sm transition-all ${
                      selectedId === source.id
                        ? 'border-brand-300 bg-brand-50 text-brand-900 shadow-sm'
                        : 'border-gray-200 bg-white text-gray-700 hover:border-gray-300 hover:shadow-sm'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-medium">{source.code}</span>
                      <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${source.enabled ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-rose-800'}`}>
                        {source.enabled ? '✓ 启用' : '✗ 停用'}
                      </span>
                    </div>
                    <p className="mt-1 text-xs text-gray-500">{source.type}</p>
                  </button>
                  <Link
                    href={`/admin/data-sources/${source.code}`}
                    className="block w-full rounded-lg bg-gray-50 px-3 py-2 text-center text-xs text-gray-600 transition hover:bg-gray-100"
                  >
                    🏢 管理公司 ({source.companies?.length || 0})
                  </Link>
                </div>
              ))}
              {data.length === 0 && (
                <div className="rounded-xl border-2 border-dashed border-gray-200 bg-gray-50 p-6 text-center">
                  <p className="text-sm text-gray-500">暂无数据源</p>
                </div>
              )}
            </div>
          </div>
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

      <DataSourceBulkUpload 
        isOpen={showBulkUpload} 
        onClose={() => setShowBulkUpload(false)} 
      />
    </div>
  );
}

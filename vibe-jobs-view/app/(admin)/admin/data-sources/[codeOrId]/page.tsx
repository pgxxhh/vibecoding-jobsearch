'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FormEvent, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';

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
  companies: DataSourceCompany[];
}

async function fetchDataSource(code: string): Promise<DataSourceResponse> {
  const res = await fetch(`/api/admin/data-sources/${code}`, { cache: 'no-store' });
  if (!res.ok) {
    throw new Error('无法获取数据源信息');
  }
  return res.json();
}

async function saveCompany(dataSourceCode: string, company: Partial<DataSourceCompany>): Promise<DataSourceCompany> {
  const url = company.id 
    ? `/api/admin/data-sources/${dataSourceCode}/companies/${company.id}`
    : `/api/admin/data-sources/${dataSourceCode}/companies`;
  const method = company.id ? 'PUT' : 'POST';
  
  const res = await fetch(url, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(company),
  });
  
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || '保存失败');
  }
  
  return res.json();
}

async function deleteCompany(dataSourceCode: string, companyId: number): Promise<void> {
  const res = await fetch(`/api/admin/data-sources/${dataSourceCode}/companies/${companyId}`, {
    method: 'DELETE',
  });
  
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || '删除失败');
  }
}

export default function DataSourceCompaniesPage({ params }: { params: { codeOrId: string } }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { data: dataSource, isLoading, isError, error } = useQuery({ 
    queryKey: ['admin', 'data-source', params.codeOrId], 
    queryFn: () => fetchDataSource(params.codeOrId) 
  });

  const [selectedCompanyId, setSelectedCompanyId] = useState<number | 'new' | null>(null);
  const [companyForm, setCompanyForm] = useState({
    reference: '',
    displayName: '',
    slug: '',
    enabled: true,
    placeholderOverridesJson: '{}',
    overrideOptionsJson: '{}',
  });
  const [message, setMessage] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const selectedCompany = dataSource?.companies.find(c => c.id === selectedCompanyId) ?? null;

  useEffect(() => {
    if (selectedCompany) {
      setCompanyForm({
        reference: selectedCompany.reference,
        displayName: selectedCompany.displayName,
        slug: selectedCompany.slug,
        enabled: selectedCompany.enabled,
        placeholderOverridesJson: JSON.stringify(selectedCompany.placeholderOverrides ?? {}, null, 2),
        overrideOptionsJson: JSON.stringify(selectedCompany.overrideOptions ?? {}, null, 2),
      });
    } else if (selectedCompanyId === 'new') {
      setCompanyForm({
        reference: '',
        displayName: '',
        slug: '',
        enabled: true,
        placeholderOverridesJson: '{}',
        overrideOptionsJson: '{}',
      });
    }
    setMessage(null);
    setErrorMsg(null);
  }, [selectedCompanyId, selectedCompany]);

  const saveCompanyMutation = useMutation({
    mutationFn: async (payload: Partial<DataSourceCompany>) => {
      return saveCompany(params.codeOrId, payload);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin', 'data-source', params.codeOrId] });
      setMessage('公司信息已保存');
      setErrorMsg(null);
      setSelectedCompanyId(null);
    },
    onError: (err: unknown) => {
      setMessage(null);
      setErrorMsg(err instanceof Error ? err.message : '保存失败');
    },
  });

  const deleteCompanyMutation = useMutation({
    mutationFn: async (companyId: number) => {
      return deleteCompany(params.codeOrId, companyId);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin', 'data-source', params.codeOrId] });
      setSelectedCompanyId(null);
      setMessage('公司已删除');
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
      const placeholderOverrides = companyForm.placeholderOverridesJson.trim() 
        ? JSON.parse(companyForm.placeholderOverridesJson) 
        : {};
      const overrideOptions = companyForm.overrideOptionsJson.trim() 
        ? JSON.parse(companyForm.overrideOptionsJson) 
        : {};

      if (!companyForm.reference) {
        throw new Error('Reference 不能为空');
      }

      const payload: Partial<DataSourceCompany> = {
        reference: companyForm.reference,
        displayName: companyForm.displayName,
        slug: companyForm.slug,
        enabled: companyForm.enabled,
        placeholderOverrides,
        overrideOptions,
      };

      if (selectedCompanyId !== 'new' && selectedCompany) {
        payload.id = selectedCompany.id;
      }

      saveCompanyMutation.mutate(payload);
    } catch (err) {
      setMessage(null);
      setErrorMsg(err instanceof Error ? err.message : '无法解析 JSON');
    }
  };

  if (isLoading) {
    return <p className="text-white/80">加载中...</p>;
  }

  if (isError || !dataSource) {
    return <p className="text-red-300">{(error as Error)?.message ?? '加载失败'}</p>;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <div className="flex items-center space-x-2 text-sm text-white/60">
            <Link href="/admin/data-sources" className="hover:text-white/80">
              数据源管理
            </Link>
            <span>/</span>
            <span>{dataSource.code}</span>
          </div>
          <h2 className="text-2xl font-semibold text-white">
            {dataSource.code} - 公司管理
          </h2>
          <p className="text-sm text-white/70">
            数据源类型: {dataSource.type} | 状态: {dataSource.enabled ? '启用' : '停用'}
          </p>
        </div>
        <button
          onClick={() => setSelectedCompanyId('new')}
          className="rounded-md bg-white/10 px-3 py-2 text-sm font-medium text-white hover:bg-white/20"
        >
          添加公司
        </button>
      </div>

      <div className="grid gap-6 lg:grid-cols-[400px_1fr]">
        <aside className="space-y-3 rounded-xl border border-white/10 bg-white/5 p-4">
          <h3 className="text-sm font-medium text-white/80">公司列表</h3>
          {dataSource.companies.map((company) => (
            <button
              key={company.id}
              onClick={() => setSelectedCompanyId(company.id)}
              className={`w-full rounded-lg border px-3 py-2 text-left text-sm transition ${
                selectedCompanyId === company.id
                  ? 'border-white/40 bg-white/15 text-white'
                  : 'border-white/10 bg-transparent text-white/70 hover:border-white/30 hover:text-white'
              }`}
            >
              <div className="flex items-center justify-between">
                <span className="font-medium">{company.displayName || company.reference}</span>
                <span className={`text-xs ${company.enabled ? 'text-emerald-300' : 'text-rose-300'}`}>
                  {company.enabled ? '启用' : '停用'}
                </span>
              </div>
              <p className="text-xs text-white/60">
                {company.reference}
              </p>
              {company.slug && (
                <p className="text-xs text-white/50">
                  Slug: {company.slug}
                </p>
              )}
            </button>
          ))}
          {dataSource.companies.length === 0 && (
            <p className="text-sm text-white/60">暂无公司配置</p>
          )}
        </aside>

        <section className="rounded-xl border border-white/10 bg-white/5 p-4">
          {selectedCompanyId === null && (
            <p className="text-sm text-white/70">
              请选择左侧公司，或点击"添加公司"。
            </p>
          )}
          {selectedCompanyId !== null && (
            <form className="space-y-4" onSubmit={handleSubmit}>
              <div className="grid gap-4 md:grid-cols-2">
                <label className="flex flex-col space-y-1 text-sm text-white/80">
                  <span>Reference *</span>
                  <input
                    value={companyForm.reference}
                    onChange={(e) => setCompanyForm((prev) => ({ ...prev, reference: e.target.value }))}
                    className="rounded-md border border-white/10 bg-white/5 px-3 py-2 text-white focus:border-white/40 focus:outline-none"
                    required
                    placeholder="公司唯一标识符"
                  />
                </label>
                <label className="flex flex-col space-y-1 text-sm text-white/80">
                  <span>Display Name</span>
                  <input
                    value={companyForm.displayName}
                    onChange={(e) => setCompanyForm((prev) => ({ ...prev, displayName: e.target.value }))}
                    className="rounded-md border border-white/10 bg-white/5 px-3 py-2 text-white focus:border-white/40 focus:outline-none"
                    placeholder="显示名称"
                  />
                </label>
                <label className="flex flex-col space-y-1 text-sm text-white/80">
                  <span>Slug</span>
                  <input
                    value={companyForm.slug}
                    onChange={(e) => setCompanyForm((prev) => ({ ...prev, slug: e.target.value }))}
                    className="rounded-md border border-white/10 bg-white/5 px-3 py-2 text-white focus:border-white/40 focus:outline-none"
                    placeholder="URL slug"
                  />
                </label>
                <label className="flex items-center space-x-2 text-sm text-white/80">
                  <input
                    type="checkbox"
                    checked={companyForm.enabled}
                    onChange={(e) => setCompanyForm((prev) => ({ ...prev, enabled: e.target.checked }))}
                    className="h-4 w-4 rounded border-white/20 bg-white/10"
                  />
                  <span>启用</span>
                </label>
              </div>

              <label className="flex flex-col space-y-1 text-sm text-white/80">
                <span>Placeholder Overrides（JSON）</span>
                <textarea
                  value={companyForm.placeholderOverridesJson}
                  onChange={(e) => setCompanyForm((prev) => ({ ...prev, placeholderOverridesJson: e.target.value }))}
                  rows={6}
                  className="rounded-md border border-white/10 bg-white/5 px-3 py-2 font-mono text-xs text-white focus:border-white/40 focus:outline-none"
                  placeholder='{"key": "value"}'
                />
              </label>

              <label className="flex flex-col space-y-1 text-sm text-white/80">
                <span>Override Options（JSON）</span>
                <textarea
                  value={companyForm.overrideOptionsJson}
                  onChange={(e) => setCompanyForm((prev) => ({ ...prev, overrideOptionsJson: e.target.value }))}
                  rows={8}
                  className="rounded-md border border-white/10 bg-white/5 px-3 py-2 font-mono text-xs text-white focus:border-white/40 focus:outline-none"
                  placeholder='{"option": "value"}'
                />
              </label>

              {message && <p className="text-sm text-emerald-300">{message}</p>}
              {errorMsg && <p className="text-sm text-rose-300">{errorMsg}</p>}

              <div className="flex items-center space-x-3">
                <button
                  type="submit"
                  disabled={saveCompanyMutation.isPending}
                  className="rounded-md bg-white/10 px-4 py-2 text-sm font-medium text-white hover:bg-white/20 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {saveCompanyMutation.isPending ? '保存中...' : '保存'}
                </button>
                {selectedCompanyId !== 'new' && selectedCompanyId !== null && (
                  <button
                    type="button"
                    onClick={() => selectedCompanyId && typeof selectedCompanyId === 'number' && deleteCompanyMutation.mutate(selectedCompanyId)}
                    disabled={deleteCompanyMutation.isPending}
                    className="rounded-md bg-rose-500/20 px-4 py-2 text-sm font-medium text-rose-200 hover:bg-rose-500/30 disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {deleteCompanyMutation.isPending ? '删除中...' : '删除'}
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => setSelectedCompanyId(null)}
                  className="rounded-md bg-white/5 px-4 py-2 text-sm font-medium text-white/70 hover:bg-white/10"
                >
                  取消
                </button>
              </div>
            </form>
          )}
        </section>
      </div>
    </div>
  );
}
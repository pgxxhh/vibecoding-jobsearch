'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FormEvent, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import CompanyBulkUpload from '@/components/admin/CompanyBulkUpload';
import Pagination from '@/components/ui/Pagination';

interface DataSourceCompany {
  id: number | null;
  reference: string;
  displayName: string;
  slug: string;
  enabled: boolean;
  placeholderOverrides: Record<string, string>;
  overrideOptions: Record<string, string>;
}

interface PagedCompanyResponse {
  content: DataSourceCompany[];
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

interface PagedDataSourceResponse {
  id: number;
  code: string;
  type: string;
  enabled: boolean;
  runOnStartup: boolean;
  requireOverride: boolean;
  flow: 'LIMITED' | 'UNLIMITED';
  baseOptions: Record<string, string>;
  companies: PagedCompanyResponse;
}

async function fetchDataSourcePaged(code: string, page: number, size: number): Promise<PagedDataSourceResponse> {
  const trimmed = code.trim();
  if (!trimmed) {
    throw new Error('数据源标识无效');
  }
  const encodedCode = encodeURIComponent(trimmed);
  const res = await fetch(`/api/admin/data-sources/${encodedCode}/paged?page=${page}&size=${size}`, { cache: 'no-store' });
  if (!res.ok) {
    throw new Error('无法获取数据源信息');
  }
  return res.json();
}

async function saveCompany(dataSourceCode: string, company: Partial<DataSourceCompany>): Promise<DataSourceCompany> {
  const url = company.id 
    ? `/api/admin/data-sources?dataSourceCode=${dataSourceCode}&companyId=${company.id}`
    : `/api/admin/data-sources?dataSourceCode=${dataSourceCode}`;
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
  const res = await fetch(`/api/admin/data-sources?dataSourceCode=${dataSourceCode}&companyId=${companyId}`, {
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
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize] = useState(10);
  
  const { data: dataSource, isLoading, isError, error } = useQuery({
    queryKey: ['admin', 'data-source-paged', params.codeOrId, currentPage, pageSize],
    queryFn: () => fetchDataSourcePaged(params.codeOrId, currentPage, pageSize)
  });

  const [selectedCompanyId, setSelectedCompanyId] = useState<number | 'new' | null>(null);
  const [showBulkUpload, setShowBulkUpload] = useState(false);
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

  const selectedCompany = dataSource?.companies.content.find(c => c.id === selectedCompanyId) ?? null;

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
      await queryClient.invalidateQueries({ queryKey: ['admin', 'data-source-paged', params.codeOrId] });
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
      const code = dataSource?.code ?? params.codeOrId;
      return deleteCompany(code, companyId);
    },
    onSuccess: async () => {
      const keys = new Set<string | number>();
      keys.add(params.codeOrId);
      if (dataSource?.code) {
        keys.add(dataSource.code);
      }
      await Promise.all(
        Array.from(keys).map((key) =>
          queryClient.invalidateQueries({ queryKey: ['admin', 'data-source-paged', key] })
        )
      );
      setSelectedCompanyId(null);
      setMessage('公司已删除');
      setErrorMsg(null);
    },
    onError: (err: unknown) => {
      setMessage(null);
      setErrorMsg(err instanceof Error ? err.message : '删除失败');
    },
  });

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
    setSelectedCompanyId(null); // Reset selection when changing pages
  };

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
          <div className="flex items-center space-x-2 text-sm text-gray-500">
            <Link href="/admin/data-sources" className="hover:text-gray-700 transition">
              数据源管理
            </Link>
            <span>/</span>
            <span className="text-gray-900">{dataSource.code}</span>
          </div>
          <h2 className="text-2xl font-semibold text-gray-900 mt-1">
            {dataSource.code} - 公司管理
          </h2>
          <p className="text-sm text-gray-600 mt-1">
            数据源类型: <span className="font-medium">{dataSource.type}</span> | 状态: <span className={`font-medium ${dataSource.enabled ? 'text-emerald-600' : 'text-rose-600'}`}>{dataSource.enabled ? '启用' : '停用'}</span>
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => setShowBulkUpload(true)}
            className="inline-flex items-center justify-center gap-2 rounded-2xl transition active:scale-[.98] h-10 px-4 text-sm bg-blue-600 text-white hover:bg-blue-700 shadow-lg focus:outline-none focus:ring-2 focus:ring-blue-500/30"
          >
            📤 批量上传
          </button>
          <button
            type="button"
            onClick={() => setSelectedCompanyId('new')}
            className="inline-flex items-center justify-center gap-2 rounded-2xl transition active:scale-[.98] h-10 px-4 text-sm bg-emerald-600 text-white hover:bg-emerald-700 shadow-lg focus:outline-none focus:ring-2 focus:ring-emerald-500/30"
          >
            🏢 添加公司
          </button>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[420px_1fr]">
        <aside className="space-y-3">
          <div className="rounded-2xl border border-gray-200 bg-white shadow-sm">
            <div className="p-4 border-b border-gray-200">
              <h3 className="text-sm font-semibold text-gray-900">公司列表</h3>
              <p className="text-xs text-gray-500 mt-1">
                共 {dataSource.companies.totalElements} 个公司
              </p>
            </div>
            <div className="p-4 space-y-2">
              {dataSource.companies.content.map((company) => (
                <button
                  key={company.id}
                  onClick={() => setSelectedCompanyId(company.id)}
                  className={`w-full rounded-xl border px-3 py-3 text-left text-sm transition-all ${
                    selectedCompanyId === company.id
                      ? 'border-brand-300 bg-brand-50 text-brand-900 shadow-sm'
                      : 'border-gray-200 bg-white text-gray-700 hover:border-gray-300 hover:shadow-sm'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="font-medium">{company.displayName || company.reference}</span>
                    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${company.enabled ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-rose-800'}`}>
                      {company.enabled ? '✓ 启用' : '✗ 停用'}
                    </span>
                  </div>
                  <p className="text-xs text-gray-500 mt-1">
                    Reference: {company.reference}
                  </p>
                  {company.slug && (
                    <p className="text-xs text-gray-400 mt-1">
                      Slug: {company.slug}
                    </p>
                  )}
                </button>
              ))}
              {dataSource.companies.content.length === 0 && (
                <div className="rounded-xl border-2 border-dashed border-gray-200 bg-gray-50 p-6 text-center">
                  <p className="text-sm text-gray-500">暂无公司配置</p>
                </div>
              )}
            </div>
            {dataSource.companies.totalPages > 1 && (
              <Pagination
                currentPage={dataSource.companies.page}
                totalPages={dataSource.companies.totalPages}
                hasNext={dataSource.companies.hasNext}
                hasPrevious={dataSource.companies.hasPrevious}
                onPageChange={handlePageChange}
                totalElements={dataSource.companies.totalElements}
                pageSize={dataSource.companies.size}
              />
            )}
          </div>
        </aside>

        <section className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
          {selectedCompanyId === null && (
            <div className="text-center py-12">
              <div className="mx-auto h-12 w-12 text-gray-400 text-4xl">
                🏢
              </div>
              <p className="mt-4 text-sm text-gray-600">
                请选择左侧公司，或点击"添加公司"。
              </p>
            </div>
          )}
          {selectedCompanyId !== null && (
            <form className="space-y-6" onSubmit={handleSubmit}>
              <div>
                <h3 className="text-lg font-semibold text-gray-900 mb-4">
                  {selectedCompanyId === 'new' ? '添加新公司' : `编辑公司: ${selectedCompany?.displayName || selectedCompany?.reference}`}
                </h3>
              </div>

              <div className="grid gap-4 md:grid-cols-2">
                <label className="flex flex-col space-y-2 text-sm">
                  <span className="font-medium text-gray-700">Reference *</span>
                  <input
                    value={companyForm.reference}
                    onChange={(e) => setCompanyForm((prev) => ({ ...prev, reference: e.target.value }))}
                    className="rounded-xl border border-gray-200 bg-white px-4 py-3 text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
                    required
                    placeholder="公司唯一标识符"
                  />
                </label>
                <label className="flex flex-col space-y-2 text-sm">
                  <span className="font-medium text-gray-700">Display Name</span>
                  <input
                    value={companyForm.displayName}
                    onChange={(e) => setCompanyForm((prev) => ({ ...prev, displayName: e.target.value }))}
                    className="rounded-xl border border-gray-200 bg-white px-4 py-3 text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
                    placeholder="显示名称"
                  />
                </label>
                <label className="flex flex-col space-y-2 text-sm">
                  <span className="font-medium text-gray-700">Slug</span>
                  <input
                    value={companyForm.slug}
                    onChange={(e) => setCompanyForm((prev) => ({ ...prev, slug: e.target.value }))}
                    className="rounded-xl border border-gray-200 bg-white px-4 py-3 text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
                    placeholder="URL slug"
                  />
                </label>
                <label className="flex items-center space-x-3 text-sm">
                  <input
                    type="checkbox"
                    checked={companyForm.enabled}
                    onChange={(e) => setCompanyForm((prev) => ({ ...prev, enabled: e.target.checked }))}
                    className="h-4 w-4 rounded border-gray-300 text-brand-600 focus:ring-brand-500"
                  />
                  <span className="font-medium text-gray-700">启用</span>
                </label>
              </div>

              <div className="grid gap-4 md:grid-cols-2">
                <label className="flex flex-col space-y-2 text-sm">
                  <span className="font-medium text-gray-700">Placeholder Overrides（JSON）</span>
                  <textarea
                    value={companyForm.placeholderOverridesJson}
                    onChange={(e) => setCompanyForm((prev) => ({ ...prev, placeholderOverridesJson: e.target.value }))}
                    rows={8}
                    className="rounded-xl border border-gray-200 bg-white px-4 py-3 font-mono text-xs text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
                    placeholder='{"key": "value"}'
                  />
                </label>

                <label className="flex flex-col space-y-2 text-sm">
                  <span className="font-medium text-gray-700">Override Options（JSON）</span>
                  <textarea
                    value={companyForm.overrideOptionsJson}
                    onChange={(e) => setCompanyForm((prev) => ({ ...prev, overrideOptionsJson: e.target.value }))}
                    rows={8}
                    className="rounded-xl border border-gray-200 bg-white px-4 py-3 font-mono text-xs text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
                    placeholder='{"option": "value"}'
                  />
                </label>
              </div>

              {message && (
                <div className="rounded-xl bg-emerald-50 border border-emerald-200 p-4">
                  <p className="text-sm text-emerald-800">✓ {message}</p>
                </div>
              )}
              {errorMsg && (
                <div className="rounded-xl bg-rose-50 border border-rose-200 p-4">
                  <p className="text-sm text-rose-800">✗ {errorMsg}</p>
                </div>
              )}

              <div className="flex items-center gap-3 pt-4 border-t border-gray-200">
                <button
                  type="submit"
                  disabled={saveCompanyMutation.isPending}
                  className="inline-flex items-center justify-center gap-2 rounded-2xl transition active:scale-[.98] disabled:cursor-not-allowed disabled:opacity-60 h-10 px-6 text-sm bg-brand-600 text-white hover:bg-brand-700 shadow-brand-sm focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-500/30"
                >
                  {saveCompanyMutation.isPending ? '保存中...' : '💾 保存'}
                </button>
                {selectedCompanyId !== 'new' && selectedCompanyId !== null && (
                  <button
                    type="button"
                    onClick={() => selectedCompanyId && typeof selectedCompanyId === 'number' && deleteCompanyMutation.mutate(selectedCompanyId)}
                    disabled={deleteCompanyMutation.isPending}
                    className="inline-flex items-center justify-center gap-2 rounded-2xl transition active:scale-[.98] disabled:cursor-not-allowed disabled:opacity-60 h-10 px-6 text-sm bg-rose-600 text-white hover:bg-rose-700 shadow-brand-sm focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-rose-500/30"
                  >
                    {deleteCompanyMutation.isPending ? '删除中...' : '🗑️ 删除'}
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => setSelectedCompanyId(null)}
                  className="inline-flex items-center justify-center gap-2 rounded-2xl transition active:scale-[.98] h-10 px-6 text-sm border border-gray-300 bg-white text-gray-700 hover:bg-gray-50 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-gray-500/15"
                >
                  取消
                </button>
              </div>
            </form>
          )}
        </section>
      </div>

      <CompanyBulkUpload 
        isOpen={showBulkUpload}
        onClose={() => setShowBulkUpload(false)}
        dataSourceCode={params.codeOrId}
      />
    </div>
  );
}
'use client';

import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
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
  const res = await fetch(`/api/admin/data-sources/${code}/paged?page=${page}&size=${size}`, { cache: 'no-store' });
  if (!res.ok) {
    throw new Error('无法获取数据源信息');
  }
  return res.json();
}

export default function DemoPaginationPage() {
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [selectedCompanyId, setSelectedCompanyId] = useState<number | null>(null);
  
  const { data: dataSource, isLoading, isError, error } = useQuery({ 
    queryKey: ['demo', 'data-source-paged', 'greenhouse', currentPage, pageSize], 
    queryFn: () => fetchDataSourcePaged('greenhouse', currentPage, pageSize)
  });

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
    setSelectedCompanyId(null);
  };

  const handlePageSizeChange = (newSize: number) => {
    setPageSize(newSize);
    setCurrentPage(0);
    setSelectedCompanyId(null);
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <p className="text-gray-600">加载中...</p>
      </div>
    );
  }

  if (isError || !dataSource) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <p className="text-red-600">{(error as Error)?.message ?? '加载失败'}</p>
      </div>
    );
  }

  const selectedCompany = dataSource?.companies.content.find(c => c.id === selectedCompanyId) ?? null;

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="mb-6">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">数据源分页演示</h1>
          <div className="flex items-center gap-4 text-sm text-gray-600">
            <span>数据源: <span className="font-medium text-gray-900">{dataSource.code}</span></span>
            <span>类型: <span className="font-medium text-gray-900">{dataSource.type}</span></span>
            <span>状态: <span className={`font-medium ${dataSource.enabled ? 'text-emerald-600' : 'text-rose-600'}`}>
              {dataSource.enabled ? '✅ 启用' : '❌ 停用'}
            </span></span>
          </div>
        </div>

        <div className="grid gap-6 lg:grid-cols-[420px_1fr]">
          <aside className="space-y-4">
            {/* 页面大小控制 */}
            <div className="rounded-2xl border border-gray-200 bg-white p-4 shadow-sm">
              <h3 className="text-sm font-semibold text-gray-900 mb-3">分页设置</h3>
              <div className="space-y-2">
                <label className="text-xs text-gray-600">每页显示数量:</label>
                <select 
                  value={pageSize}
                  onChange={(e) => handlePageSizeChange(Number(e.target.value))}
                  className="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
                >
                  <option value={5}>5 个/页</option>
                  <option value={10}>10 个/页</option>
                  <option value={20}>20 个/页</option>
                  <option value={50}>50 个/页</option>
                </select>
              </div>
            </div>

            {/* 公司列表 */}
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
                        ? 'border-blue-300 bg-blue-50 text-blue-900 shadow-sm'
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
              {/* 分页组件 */}
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
                <div className="mx-auto h-16 w-16 text-gray-400 text-5xl mb-4">
                  🏢
                </div>
                <h3 className="text-lg font-semibold text-gray-900 mb-2">选择公司查看详情</h3>
                <p className="text-sm text-gray-600">
                  请从左侧公司列表中选择一个公司来查看详细信息。
                </p>
                <div className="mt-6 text-xs text-gray-500 bg-gray-50 rounded-xl p-4">
                  <p><strong>演示说明:</strong> 这是一个分页功能演示页面</p>
                  <p className="mt-1">• 左侧显示分页的公司列表</p>
                  <p>• 可以调整每页显示的数量</p>
                  <p>• 底部有完整的分页导航</p>
                  <p>• 点击公司可查看详情</p>
                </div>
              </div>
            )}
            {selectedCompany && (
              <div className="space-y-6">
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-4">
                    公司详情: {selectedCompany.displayName || selectedCompany.reference}
                  </h3>
                </div>

                <div className="grid gap-4 md:grid-cols-2">
                  <div className="space-y-2">
                    <label className="text-sm font-medium text-gray-700">Reference</label>
                    <div className="rounded-xl border border-gray-200 bg-gray-50 px-4 py-3 text-gray-900">
                      {selectedCompany.reference}
                    </div>
                  </div>
                  <div className="space-y-2">
                    <label className="text-sm font-medium text-gray-700">Display Name</label>
                    <div className="rounded-xl border border-gray-200 bg-gray-50 px-4 py-3 text-gray-900">
                      {selectedCompany.displayName || '-'}
                    </div>
                  </div>
                  <div className="space-y-2">
                    <label className="text-sm font-medium text-gray-700">Slug</label>
                    <div className="rounded-xl border border-gray-200 bg-gray-50 px-4 py-3 text-gray-900">
                      {selectedCompany.slug || '-'}
                    </div>
                  </div>
                  <div className="space-y-2">
                    <label className="text-sm font-medium text-gray-700">状态</label>
                    <div className={`rounded-xl border px-4 py-3 font-medium ${
                      selectedCompany.enabled 
                        ? 'border-emerald-200 bg-emerald-50 text-emerald-800' 
                        : 'border-rose-200 bg-rose-50 text-rose-800'
                    }`}>
                      {selectedCompany.enabled ? '✅ 启用' : '❌ 停用'}
                    </div>
                  </div>
                </div>

                <div className="pt-4 border-t border-gray-200">
                  <button
                    type="button"
                    onClick={() => setSelectedCompanyId(null)}
                    className="inline-flex items-center justify-center gap-2 rounded-2xl transition active:scale-[.98] h-10 px-6 text-sm border border-gray-300 bg-white text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-500/20"
                  >
                    返回列表
                  </button>
                </div>
              </div>
            )}
          </section>
        </div>
      </div>
    </div>
  );
}
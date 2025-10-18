import type {
  CompanyBulkPayload,
  DataSourcePayload,
  DataSourceResponse,
  PagedDataSourceResponse,
} from '@/modules/admin/types';

function toJson(response: Response) {
  if (!response.ok) {
    return response.text().then((text) => {
      throw new Error(text || '请求失败');
    });
  }
  return response.json();
}

export async function fetchDataSources(): Promise<DataSourceResponse[]> {
  const res = await fetch('/api/admin/data-sources', { cache: 'no-store' });
  return toJson(res);
}

export async function saveDataSource(payload: DataSourcePayload, id?: number | 'new' | null) {
  const isNew = !id || id === 'new';
  const url = isNew ? '/api/admin/data-sources' : `/api/admin/data-sources?id=${id}`;
  const method = isNew ? 'POST' : 'PUT';
  const res = await fetch(url, {
    method,
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(payload),
  });
  return toJson(res);
}

export async function deleteDataSource(id: number) {
  const res = await fetch(`/api/admin/data-sources?id=${id}`, { method: 'DELETE' });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || '删除失败');
  }
}

export async function bulkUploadDataSources(dataSources: DataSourcePayload[]) {
  const res = await fetch('/api/admin/data-sources/bulk', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ dataSources }),
  });
  return toJson(res);
}

export async function fetchDataSourcePaged(codeOrId: string, page: number, size: number): Promise<PagedDataSourceResponse> {
  const trimmed = codeOrId.trim();
  if (!trimmed) {
    throw new Error('数据源标识无效');
  }
  const encoded = encodeURIComponent(trimmed);
  const res = await fetch(`/api/admin/data-sources/${encoded}/paged?page=${page}&size=${size}`, { cache: 'no-store' });
  return toJson(res);
}

export async function saveCompany(dataSourceCode: string, company: Partial<CompanyBulkPayload> & { id?: number | null }) {
  const url = company.id != null
    ? `/api/admin/data-sources?dataSourceCode=${dataSourceCode}&companyId=${company.id}`
    : `/api/admin/data-sources?dataSourceCode=${dataSourceCode}`;
  const method = company.id != null ? 'PUT' : 'POST';
  const res = await fetch(url, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(company),
  });
  return toJson(res);
}

export async function deleteCompany(dataSourceCode: string, companyId: number) {
  const res = await fetch(`/api/admin/data-sources?dataSourceCode=${dataSourceCode}&companyId=${companyId}`, {
    method: 'DELETE',
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || '删除失败');
  }
}

export async function bulkUploadCompanies(dataSourceCode: string, companies: CompanyBulkPayload[]) {
  const res = await fetch(`/api/admin/data-sources/${dataSourceCode}/companies/bulk`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ companies }),
  });
  return toJson(res);
}

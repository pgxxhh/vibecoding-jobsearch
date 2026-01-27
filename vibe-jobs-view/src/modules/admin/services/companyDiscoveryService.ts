import type {
  CompanyDiscoveryResult,
  CompanyDiscoveryRun,
  CompanyDiscoverySettings,
} from '@/modules/admin/types';

function toJson(response: Response) {
  if (!response.ok) {
    return response.text().then((text) => {
      throw new Error(text || '请求失败');
    });
  }
  return response.json();
}

export async function fetchCompanyDiscoverySettings(): Promise<CompanyDiscoverySettings> {
  const res = await fetch('/api/admin/company-discovery/settings', { cache: 'no-store' });
  return toJson(res);
}

export async function updateCompanyDiscoverySettings(payload: Partial<CompanyDiscoverySettings>) {
  const res = await fetch('/api/admin/company-discovery/settings', {
    method: 'PUT',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(payload),
  });
  return toJson(res);
}

export async function fetchCompanyDiscoveryRuns(): Promise<CompanyDiscoveryRun[]> {
  const res = await fetch('/api/admin/company-discovery/runs', { cache: 'no-store' });
  return toJson(res);
}

export async function fetchCompanyDiscoveryResults(): Promise<CompanyDiscoveryResult[]> {
  const res = await fetch('/api/admin/company-discovery/results', { cache: 'no-store' });
  return toJson(res);
}

export async function triggerCompanyDiscoveryRun(): Promise<CompanyDiscoveryRun> {
  const res = await fetch('/api/admin/company-discovery/run', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
  });
  return toJson(res);
}

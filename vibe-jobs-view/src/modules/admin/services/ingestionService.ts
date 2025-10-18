import type { IngestionSettings } from '@/modules/admin/types';

function toJson(response: Response) {
  if (!response.ok) {
    return response.text().then((text) => {
      throw new Error(text || '请求失败');
    });
  }
  return response.json();
}

export async function fetchIngestionSettings(): Promise<IngestionSettings> {
  const res = await fetch('/api/admin/ingestion-settings', { cache: 'no-store' });
  return toJson(res);
}

export async function updateIngestionSettings(payload: Partial<IngestionSettings>) {
  const res = await fetch('/api/admin/ingestion-settings', {
    method: 'PUT',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(payload),
  });
  return toJson(res);
}

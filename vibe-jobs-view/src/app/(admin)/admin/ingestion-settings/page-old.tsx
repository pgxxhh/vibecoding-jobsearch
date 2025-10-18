'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FormEvent, useEffect, useState } from 'react';

interface IngestionSettingsResponse {
  fixedDelayMs: number;
  initialDelayMs: number;
  pageSize: number;
  mode: 'RECENT' | 'COMPANIES';
  companies: string[];
  recentDays: number;
  concurrency: number;
  companyOverrides: Record<string, unknown>;
  locationFilter: unknown;
  roleFilter: unknown;
  updatedAt: string;
}

async function fetchSettings(): Promise<IngestionSettingsResponse> {
  const res = await fetch('/api/admin/ingestion-settings', { cache: 'no-store' });
  if (!res.ok) {
    throw new Error('无法获取采集配置');
  }
  return res.json();
}

export default function IngestionSettingsPage() {
  const queryClient = useQueryClient();
  const { data, isLoading, isError, error } = useQuery({ queryKey: ['admin', 'ingestion-settings'], queryFn: fetchSettings });
  const [fixedDelayMs, setFixedDelayMs] = useState('3600000');
  const [initialDelayMs, setInitialDelayMs] = useState('10000');
  const [pageSize, setPageSize] = useState('100');
  const [recentDays, setRecentDays] = useState('7');
  const [concurrency, setConcurrency] = useState('4');
  const [mode, setMode] = useState<'RECENT' | 'COMPANIES'>('RECENT');
  const [companiesText, setCompaniesText] = useState('');
  const [locationJson, setLocationJson] = useState('');
  const [roleJson, setRoleJson] = useState('');
  const [message, setMessage] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useEffect(() => {
    if (data) {
      setFixedDelayMs(String(data.fixedDelayMs));
      setInitialDelayMs(String(data.initialDelayMs));
      setPageSize(String(data.pageSize));
      setRecentDays(String(data.recentDays));
      setConcurrency(String(data.concurrency));
      setMode(data.mode);
      setCompaniesText((data.companies || []).join('\n'));
      setLocationJson(JSON.stringify(data.locationFilter ?? {}, null, 2));
      setRoleJson(JSON.stringify(data.roleFilter ?? {}, null, 2));
      setMessage(null);
      setErrorMsg(null);
    }
  }, [data]);

  const mutation = useMutation({
    mutationFn: async (payload: Record<string, unknown>) => {
      const res = await fetch('/api/admin/ingestion-settings', {
        method: 'PUT',
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
      await queryClient.invalidateQueries({ queryKey: ['admin', 'ingestion-settings'] });
      setMessage('保存成功，新的调度配置将在几秒内生效。');
      setErrorMsg(null);
    },
    onError: (err: unknown) => {
      setMessage(null);
      setErrorMsg(err instanceof Error ? err.message : '保存失败');
    },
  });

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!data) return;

    try {
      const location = locationJson.trim() ? JSON.parse(locationJson) : {};
      const role = roleJson.trim() ? JSON.parse(roleJson) : {};
      const companies = companiesText
        .split(/\r?\n/)
        .map((item) => item.trim())
        .filter(Boolean);

      const payload = {
        fixedDelayMs: Number(fixedDelayMs) || data.fixedDelayMs,
        initialDelayMs: Number(initialDelayMs) || data.initialDelayMs,
        pageSize: Number(pageSize) || data.pageSize,
        mode,
        companies,
        recentDays: Number(recentDays) || data.recentDays,
        concurrency: Number(concurrency) || data.concurrency,
        companyOverrides: data.companyOverrides ?? {},
        locationFilter: location,
        roleFilter: role,
      };
      mutation.mutate(payload);
    } catch (err) {
      setMessage(null);
      setErrorMsg(err instanceof Error ? err.message : '无法解析 JSON 配置');
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
      <div>
        <h2 className="text-2xl font-semibold text-white">采集调度配置</h2>
        <p className="text-sm text-white/70">修改后保存即可，后台会自动调整定时任务与线程池。</p>
      </div>
      <form className="space-y-6" onSubmit={handleSubmit}>
        <div className="grid gap-4 md:grid-cols-2">
          <label className="flex flex-col space-y-2 text-sm text-white/80">
            <span>固定延迟（毫秒）</span>
            <input
              value={fixedDelayMs}
              onChange={(e) => setFixedDelayMs(e.target.value)}
              className="rounded-md border border-white/10 bg-white/5 px-3 py-2 text-white focus:border-white/40 focus:outline-none"
              type="number"
              min={1000}
              step={1000}
            />
          </label>
          <label className="flex flex-col space-y-2 text-sm text-white/80">
            <span>初始延迟（毫秒）</span>
            <input
              value={initialDelayMs}
              onChange={(e) => setInitialDelayMs(e.target.value)}
              className="rounded-md border border-white/10 bg-white/5 px-3 py-2 text-white focus:border-white/40 focus:outline-none"
              type="number"
              min={0}
              step={1000}
            />
          </label>
          <label className="flex flex-col space-y-2 text-sm text-white/80">
            <span>分页大小</span>
            <input
              value={pageSize}
              onChange={(e) => setPageSize(e.target.value)}
              className="rounded-md border border-white/10 bg-white/5 px-3 py-2 text-white focus:border-white/40 focus:outline-none"
              type="number"
              min={1}
            />
          </label>
          <label className="flex flex-col space-y-2 text-sm text-white/80">
            <span>并发线程数</span>
            <input
              value={concurrency}
              onChange={(e) => setConcurrency(e.target.value)}
              className="rounded-md border border-white/10 bg-white/5 px-3 py-2 text-white focus:border-white/40 focus:outline-none"
              type="number"
              min={1}
            />
          </label>
          <label className="flex flex-col space-y-2 text-sm text-white/80">
            <span>模式</span>
            <select
              value={mode}
              onChange={(e) => setMode(e.target.value as 'RECENT' | 'COMPANIES')}
              className="rounded-md border border-white/10 bg-white/5 px-3 py-2 text-white focus:border-white/40 focus:outline-none"
            >
              <option value="RECENT">RECENT（按最近天数）</option>
              <option value="COMPANIES">COMPANIES（按公司白名单）</option>
            </select>
          </label>
          <label className="flex flex-col space-y-2 text-sm text-white/80">
            <span>最近天数（RECENT 模式）</span>
            <input
              value={recentDays}
              onChange={(e) => setRecentDays(e.target.value)}
              className="rounded-md border border-white/10 bg-white/5 px-3 py-2 text-white focus:border-white/40 focus:outline-none"
              type="number"
              min={1}
            />
          </label>
        </div>

        <div className="grid gap-4 md:grid-cols-2">
          <label className="flex flex-col space-y-2 text-sm text-white/80">
            <span>公司白名单（每行一个）</span>
            <textarea
              value={companiesText}
              onChange={(e) => setCompaniesText(e.target.value)}
              rows={6}
              className="rounded-md border border-white/10 bg-white/5 px-3 py-2 font-mono text-sm text-white focus:border-white/40 focus:outline-none"
            />
          </label>
          <div className="space-y-4">
            <div>
              <span className="block text-sm text-white/80">最近更新时间</span>
              <p className="text-sm text-white/60">{new Date(data.updatedAt).toLocaleString()}</p>
            </div>
            <div className="rounded-lg border border-white/10 bg-white/5 p-3 text-xs text-white/60">
              <p>说明：</p>
              <ul className="mt-2 list-disc space-y-1 pl-5">
                <li>调度参数会在保存后自动推送给调度线程。</li>
                <li>并发度调整会实时更新线程池，无需重启。</li>
              </ul>
            </div>
          </div>
        </div>

        <div className="grid gap-4 md:grid-cols-2">
          <label className="flex flex-col space-y-2 text-sm text-white/80">
            <span>Location Filter（JSON）</span>
            <textarea
              value={locationJson}
              onChange={(e) => setLocationJson(e.target.value)}
              rows={10}
              className="rounded-md border border-white/10 bg-white/5 px-3 py-2 font-mono text-xs text-white focus:border-white/40 focus:outline-none"
            />
          </label>
          <label className="flex flex-col space-y-2 text-sm text-white/80">
            <span>Role Filter（JSON）</span>
            <textarea
              value={roleJson}
              onChange={(e) => setRoleJson(e.target.value)}
              rows={10}
              className="rounded-md border border-white/10 bg-white/5 px-3 py-2 font-mono text-xs text-white focus:border-white/40 focus:outline-none"
            />
          </label>
        </div>

        {message && <p className="text-sm text-emerald-300">{message}</p>}
        {errorMsg && <p className="text-sm text-rose-300">{errorMsg}</p>}

        <button
          type="submit"
          disabled={mutation.isPending}
          className="rounded-lg bg-white/10 px-4 py-2 text-sm font-medium text-white transition hover:bg-white/20 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {mutation.isPending ? '保存中...' : '保存配置'}
        </button>
      </form>
    </div>
  );
}

'use client';

import { FormEvent, useEffect, useState } from 'react';

import { useIngestionSettings } from '@/modules/admin/hooks/useIngestionSettings';
import {
  buildIngestionSettingsPayload,
  submitIngestionSettingsForm,
  type IngestionFormState,
} from '@/modules/admin/utils/ingestionForm';

export default function IngestionSettingsPage() {
  const { query, update } = useIngestionSettings();
  const { data, isLoading, isError, error } = query;

  const [fixedDelayMs, setFixedDelayMs] = useState('3600000');
  const [initialDelayMs, setInitialDelayMs] = useState('10000');
  const [pageSize, setPageSize] = useState('100');
  const [recentDays, setRecentDays] = useState('7');
  const [concurrency, setConcurrency] = useState('4');
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
      setLocationJson(JSON.stringify(data.locationFilter ?? {}, null, 2));
      setRoleJson(JSON.stringify(data.roleFilter ?? {}, null, 2));
    }
  }, [data]);

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formState: IngestionFormState = {
      fixedDelayMs,
      initialDelayMs,
      pageSize,
      recentDays,
      concurrency,
      locationJson,
      roleJson,
    };

    try {
      submitIngestionSettingsForm(formState, {
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
      <div>
        <h2 className="text-2xl font-semibold text-gray-900">采集调度配置</h2>
        <p className="text-sm text-gray-600 mt-1">修改后保存即可，后台会自动调整定时任务与线程池。</p>
        <div className="mt-2 text-xs text-gray-500">
          最后更新: {new Date(data.updatedAt).toLocaleString('zh-CN')}
        </div>
      </div>

      <form className="space-y-6" onSubmit={handleSubmit}>
        {/* 基础配置 */}
        <div className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">⏱️ 基础配置</h3>
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
              <span className="text-xs text-gray-500">
                {Number(fixedDelayMs) ? `约 ${Math.round(Number(fixedDelayMs) / 60000)} 分钟` : ''}
              </span>
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
              <span className="text-xs text-gray-500">启动后延迟多长时间开始第一次采集</span>
            </label>
            <label className="flex flex-col space-y-2 text-sm">
              <span className="font-medium text-gray-700">页面大小*</span>
              <input
                value={pageSize}
                onChange={(e) => setPageSize(e.target.value)}
                className="rounded-xl border border-gray-200 bg-white px-4 py-3 text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
                type="number"
                min={10}
                max={500}
                required
              />
              <span className="text-xs text-gray-500">每次请求获取的职位数量</span>
            </label>
            <label className="flex flex-col space-y-2 text-sm">
              <span className="font-medium text-gray-700">并发度*</span>
              <input
                value={concurrency}
                onChange={(e) => setConcurrency(e.target.value)}
                className="rounded-xl border border-gray-200 bg-white px-4 py-3 text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
                type="number"
                min={1}
                max={20}
                required
              />
              <span className="text-xs text-gray-500">同时执行的采集线程数</span>
            </label>
          </div>
        </div>

        {/* 采集配置 */}
        <div className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">🎯 采集配置</h3>
          <div className="space-y-4">
            <label className="flex flex-col space-y-2 text-sm">
              <span className="font-medium text-gray-700">采集时间范围（天数）*</span>
              <input
                value={recentDays}
                onChange={(e) => setRecentDays(e.target.value)}
                className="rounded-xl border border-gray-200 bg-white px-4 py-3 text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15 max-w-xs"
                type="number"
                min={1}
                max={90}
                required
              />
              <span className="text-xs text-gray-500">只采集最近 {recentDays} 天更新且来自启用公司的职位</span>
            </label>
          </div>
        </div>

        {/* 过滤器配置 */}
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
                placeholder={`{\\n  "include": ["北京", "上海"],\\n  "exclude": ["实习"]\\n}`}
              />
            </label>

            <label className="flex flex-col space-y-2 text-sm">
              <span className="font-medium text-gray-700">职位过滤器（JSON）</span>
              <textarea
                value={roleJson}
                onChange={(e) => setRoleJson(e.target.value)}
                rows={8}
                className="rounded-xl border border-gray-200 bg-white px-4 py-3 font-mono text-xs text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
                placeholder={`{\\n  "keywords": ["后端", "Java"],\\n  "exclude": ["实习", "兼职"]\\n}`}
              />
            </label>
          </div>
        </div>

        {/* 消息反馈 */}
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

        {/* 操作按钮 */}
        <div className="flex items-center gap-3 pt-4">
          <button
            type="submit"
            disabled={update.isPending}
            className="inline-flex items-center justify-center gap-2 rounded-2xl transition active:scale-[.98] disabled:cursor-not-allowed disabled:opacity-60 h-12 px-8 text-sm font-medium bg-brand-600 text-white hover:bg-brand-700 shadow-brand-sm focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-500/30"
          >
            {update.isPending ? (
              <>
                <div className="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white"></div>
                保存中...
              </>
            ) : (
              <>⚡ 保存配置</>
            )}
          </button>
          
          <div className="text-xs text-gray-500">
            💡 提示：保存后系统会自动重新调度采集任务，无需手动重启
          </div>
        </div>
      </form>
    </div>
  );
}

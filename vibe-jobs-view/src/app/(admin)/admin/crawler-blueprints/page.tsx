'use client';

import Link from 'next/link';
import { useMemo } from 'react';

import { useCrawlerBlueprints } from '@/modules/admin/hooks/useCrawlerBlueprints';
import type { CrawlerBlueprintSummary } from '@/modules/admin/types';
import Badge from '@/shared/ui/Badge';
import Button from '@/shared/ui/Button';

function formatDateTime(value?: string | null) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('zh-CN', {
    hour12: false,
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date);
}

function statusTone(status?: string | null) {
  if (!status) return 'muted' as const;
  const normalized = status.toUpperCase();
  if (normalized === 'SUCCESS') return 'brand' as const;
  if (normalized === 'FAILED') return 'default' as const;
  if (normalized === 'RUNNING' || normalized === 'PENDING') return 'brand' as const;
  return 'muted' as const;
}

export default function CrawlerBlueprintListPage() {
  const { list, rerun, activate } = useCrawlerBlueprints();
  const { data, isLoading, isError, error } = list;

  const sorted = useMemo(() => {
    if (!data) return [] as CrawlerBlueprintSummary[];
    return [...data].sort((a, b) => a.code.localeCompare(b.code));
  }, [data]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="text-center">
          <div className="mx-auto h-8 w-8 animate-spin rounded-full border-4 border-brand-200 border-t-brand-600"></div>
          <p className="mt-4 text-lg font-semibold text-gray-900">加载蓝图列表...</p>
        </div>
      </div>
    );
  }

  if (isError || !data) {
    return (
      <div className="rounded-xl bg-rose-50 border border-rose-200 p-6">
        <p className="text-rose-800">{(error as Error)?.message ?? '无法加载爬虫蓝图'}</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">爬虫蓝图</h1>
          <p className="mt-1 text-sm text-gray-600">管理爬虫自动化脚本、执行状态以及测试产物。</p>
        </div>
        <Link
          href="/admin/crawler-blueprints/new"
          className="inline-flex items-center justify-center gap-2 rounded-2xl transition active:scale-[.98] h-10 px-4 text-sm bg-brand-600 text-white hover:bg-brand-700 shadow-brand-sm focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-500/30"
        >
          ➕ 新建蓝图
        </Link>
      </div>

      <div className="grid gap-4">
        {sorted.map((blueprint) => {
          const running = rerun.isPending || activate.isPending;
          return (
            <div
              key={blueprint.code}
              className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm transition hover:border-brand-200/80 hover:shadow-md"
            >
              <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <h2 className="text-xl font-semibold text-gray-900">{blueprint.name}</h2>
                    <Badge tone="muted">{blueprint.code}</Badge>
                    <Badge tone={statusTone(blueprint.lastRunStatus)}>
                      {blueprint.lastRunStatus ? blueprint.lastRunStatus : '无执行记录'}
                    </Badge>
                    <Badge tone={blueprint.enabled ? 'brand' : 'default'}>{blueprint.enabled ? '已启用' : '已停用'}</Badge>
                  </div>
                  {blueprint.description && (
                    <p className="mt-2 text-sm text-gray-600">{blueprint.description}</p>
                  )}
                  <dl className="mt-3 grid gap-2 text-sm text-gray-600 sm:grid-cols-2">
                    <div>
                      <dt className="font-medium text-gray-700">入口 URL</dt>
                      <dd className="text-gray-600 break-all">{blueprint.entryUrl || '—'}</dd>
                    </div>
                    <div>
                      <dt className="font-medium text-gray-700">并发限制</dt>
                      <dd>{blueprint.concurrencyLimit ?? '—'}</dd>
                    </div>
                    <div>
                      <dt className="font-medium text-gray-700">最近完成</dt>
                      <dd>{formatDateTime(blueprint.lastRunFinishedAt)}</dd>
                    </div>
                    <div>
                      <dt className="font-medium text-gray-700">解析模板</dt>
                      <dd>{blueprint.parserTemplateCode || '—'}</dd>
                    </div>
                  </dl>
                </div>
                <div className="flex flex-col items-stretch gap-2 md:items-end">
                  <Link
                    href={`/admin/crawler-blueprints/${encodeURIComponent(blueprint.code)}`}
                    className="inline-flex items-center justify-center gap-2 rounded-2xl border border-gray-200 px-4 py-2 text-sm text-gray-700 transition hover:border-brand-200 hover:text-brand-700"
                  >
                    📄 查看详情
                  </Link>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => activate.mutate({ code: blueprint.code, enabled: !blueprint.enabled })}
                    disabled={running}
                  >
                    {blueprint.enabled ? '🚫 暂停蓝图' : '✅ 激活蓝图'}
                  </Button>
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => rerun.mutate({ code: blueprint.code })}
                    disabled={rerun.isPending}
                  >
                    🔁 立即重跑
                  </Button>
                  {blueprint.activeTaskId && (
                    <p className="text-xs text-brand-700">进行中的任务 {blueprint.activeTaskId}</p>
                  )}
                </div>
              </div>
            </div>
          );
        })}

        {sorted.length === 0 && (
          <div className="rounded-2xl border-2 border-dashed border-gray-200 bg-gray-50 p-12 text-center text-sm text-gray-600">
            暂无爬虫蓝图，点击右上角“新建蓝图”开始配置。
          </div>
        )}
      </div>
    </div>
  );
}

'use client';

import Link from 'next/link';

import { useCrawlerBlueprintDetail, useCrawlerBlueprints } from '@/modules/admin/hooks/useCrawlerBlueprints';
import type { CrawlerBlueprintDetail, CrawlerBlueprintRun, CrawlerBlueprintTestReport } from '@/modules/admin/types';
import Badge from '@/shared/ui/Badge';
import Button from '@/shared/ui/Button';

function formatDateTime(value?: string | null) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('zh-CN', {
    dateStyle: 'medium',
    timeStyle: 'short',
    hour12: false,
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

function RunsTable({ runs }: { runs: CrawlerBlueprintRun[] }) {
  if (runs.length === 0) {
    return <p className="text-sm text-gray-600">暂无运行记录。</p>;
  }
  return (
    <div className="overflow-auto">
      <table className="min-w-full divide-y divide-gray-200 text-left text-sm">
        <thead>
          <tr className="text-xs uppercase tracking-wider text-gray-500">
            <th className="px-4 py-2">运行 ID</th>
            <th className="px-4 py-2">状态</th>
            <th className="px-4 py-2">开始时间</th>
            <th className="px-4 py-2">结束时间</th>
            <th className="px-4 py-2">成功/失败</th>
            <th className="px-4 py-2">备注</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {runs.map((run) => (
            <tr key={run.id} className="bg-white">
              <td className="whitespace-nowrap px-4 py-2 font-mono text-xs text-gray-700">{run.id}</td>
              <td className="px-4 py-2"><Badge tone={statusTone(run.status)}>{run.status}</Badge></td>
              <td className="px-4 py-2 text-gray-600">{formatDateTime(run.startedAt)}</td>
              <td className="px-4 py-2 text-gray-600">{formatDateTime(run.finishedAt)}</td>
              <td className="px-4 py-2 text-gray-600">
                {run.successCount ?? 0} / {run.failureCount ?? 0}
              </td>
              <td className="px-4 py-2 text-xs text-gray-500">{run.message || '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function TestReports({ reports }: { reports: CrawlerBlueprintTestReport[] }) {
  if (!reports.length) {
    return <p className="text-sm text-gray-600">暂无测试报告。</p>;
  }
  return (
    <div className="space-y-3">
      {reports.map((report) => (
        <div key={report.id} className="rounded-2xl border border-gray-200 bg-white p-4 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <span className="font-medium text-gray-900">测试报告 {report.id}</span>
              <Badge tone={statusTone(report.status)}>{report.status}</Badge>
            </div>
            <span className="text-xs text-gray-500">{formatDateTime(report.createdAt)}</span>
          </div>
          {report.summary && <p className="mt-2 text-sm text-gray-600">{report.summary}</p>}
          {report.details && (
            <pre className="mt-3 max-h-48 overflow-auto rounded-xl bg-gray-50 p-3 text-xs text-gray-700">
              {typeof report.details === 'string' ? report.details : JSON.stringify(report.details, null, 2)}
            </pre>
          )}
        </div>
      ))}
    </div>
  );
}

export default function CrawlerBlueprintDetailPage({ params }: { params: { code: string } }) {
  const { rerun, activate } = useCrawlerBlueprints();
  const detail = useCrawlerBlueprintDetail(params.code);

  if (detail.isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="text-center">
          <div className="mx-auto h-8 w-8 animate-spin rounded-full border-4 border-brand-200 border-t-brand-600"></div>
          <p className="mt-4 text-lg font-semibold text-gray-900">加载蓝图详情...</p>
        </div>
      </div>
    );
  }

  if (detail.isError || !detail.data) {
    return (
      <div className="rounded-xl bg-rose-50 border border-rose-200 p-6">
        <p className="text-rose-800">{(detail.error as Error)?.message ?? '未找到该蓝图'}</p>
      </div>
    );
  }

  const blueprint: CrawlerBlueprintDetail = detail.data;
  const isRunning = rerun.isPending || activate.isPending;

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-2xl font-semibold text-gray-900">{blueprint.name}</h1>
            <Badge tone="muted">{blueprint.code}</Badge>
            <Badge tone={statusTone(blueprint.lastRunStatus)}>{blueprint.lastRunStatus ?? '无记录'}</Badge>
            <Badge tone={blueprint.enabled ? 'brand' : 'default'}>{blueprint.enabled ? '已启用' : '已停用'}</Badge>
          </div>
          {blueprint.description && <p className="mt-2 text-sm text-gray-600">{blueprint.description}</p>}
          <div className="mt-3 flex flex-wrap gap-3 text-xs text-gray-500">
            <span>创建时间：{formatDateTime(blueprint.createdAt)}</span>
            <span>更新时间：{formatDateTime(blueprint.updatedAt)}</span>
            {blueprint.activeTask && (
              <span className="text-brand-700">运行中任务：{blueprint.activeTask.id}</span>
            )}
          </div>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <Link
            href="/admin/crawler-blueprints"
            className="inline-flex items-center justify-center gap-2 rounded-2xl border border-gray-200 px-4 py-2 text-sm text-gray-700 transition hover:border-brand-200 hover:text-brand-700"
          >
            ← 返回列表
          </Link>
          <Button
            variant="outline"
            size="sm"
            onClick={() => activate.mutate({ code: blueprint.code, enabled: !blueprint.enabled })}
            disabled={isRunning}
          >
            {blueprint.enabled ? '暂停蓝图' : '启用蓝图'}
          </Button>
          <Button
            variant="primary"
            size="sm"
            onClick={() => rerun.mutate({ code: blueprint.code })}
            disabled={rerun.isPending}
          >
            触发重跑
          </Button>
        </div>
      </div>

      <section className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-gray-900">蓝图信息</h2>
        <dl className="mt-4 grid gap-4 text-sm text-gray-700 md:grid-cols-2">
          <div>
            <dt className="font-medium text-gray-900">入口 URL</dt>
            <dd className="mt-1 break-all text-gray-700">{blueprint.entryUrl ?? '—'}</dd>
          </div>
          <div>
            <dt className="font-medium text-gray-900">解析模板</dt>
            <dd className="mt-1 text-gray-700">{blueprint.parserTemplateCode ?? '—'}</dd>
          </div>
          <div>
            <dt className="font-medium text-gray-900">并发限制</dt>
            <dd className="mt-1 text-gray-700">{blueprint.concurrencyLimit ?? '—'}</dd>
          </div>
          <div>
            <dt className="font-medium text-gray-900">最近完成</dt>
            <dd className="mt-1 text-gray-700">{formatDateTime(blueprint.lastRunFinishedAt)}</dd>
          </div>
        </dl>
        {blueprint.metrics && (
          <div className="mt-6 grid gap-4 text-sm text-gray-700 sm:grid-cols-2 lg:grid-cols-4">
            <div className="rounded-2xl border border-gray-100 bg-gray-50 p-4">
              <div className="text-xs text-gray-500">累计运行</div>
              <div className="mt-2 text-xl font-semibold text-gray-900">{blueprint.metrics.totalRuns ?? '—'}</div>
            </div>
            <div className="rounded-2xl border border-gray-100 bg-gray-50 p-4">
              <div className="text-xs text-gray-500">平均耗时</div>
              <div className="mt-2 text-xl font-semibold text-gray-900">
                {blueprint.metrics.averageDurationMs ? `${Math.round(blueprint.metrics.averageDurationMs / 1000)} 秒` : '—'}
              </div>
            </div>
            <div className="rounded-2xl border border-gray-100 bg-gray-50 p-4">
              <div className="text-xs text-gray-500">成功率</div>
              <div className="mt-2 text-xl font-semibold text-gray-900">
                {blueprint.metrics.successRate != null ? `${(blueprint.metrics.successRate * 100).toFixed(1)}%` : '—'}
              </div>
            </div>
            <div className="rounded-2xl border border-gray-100 bg-gray-50 p-4">
              <div className="text-xs text-gray-500">最近一次耗时</div>
              <div className="mt-2 text-xl font-semibold text-gray-900">
                {blueprint.metrics.lastRunDurationMs ? `${Math.round(blueprint.metrics.lastRunDurationMs / 1000)} 秒` : '—'}
              </div>
            </div>
          </div>
        )}
      </section>

      {blueprint.flow && blueprint.flow.length > 0 && (
        <section className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
          <h2 className="text-lg font-semibold text-gray-900">执行流程</h2>
          <div className="mt-4 grid gap-4 md:grid-cols-2">
            {blueprint.flow.map((step) => (
              <div key={step.step} className="rounded-2xl border border-gray-100 bg-gray-50 p-4">
                <div className="flex items-center justify-between">
                  <span className="font-medium text-gray-900">{step.step}</span>
                  {step.status && <Badge tone={statusTone(step.status)}>{step.status}</Badge>}
                </div>
                {step.description && <p className="mt-2 text-sm text-gray-600">{step.description}</p>}
              </div>
            ))}
          </div>
        </section>
      )}

      <section className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm space-y-6">
        <div>
          <h2 className="text-lg font-semibold text-gray-900">最近运行</h2>
          <RunsTable runs={blueprint.latestRuns ?? []} />
        </div>
        <div>
          <h2 className="text-lg font-semibold text-gray-900">测试报告</h2>
          <TestReports reports={blueprint.testReports ?? []} />
        </div>
      </section>
    </div>
  );
}

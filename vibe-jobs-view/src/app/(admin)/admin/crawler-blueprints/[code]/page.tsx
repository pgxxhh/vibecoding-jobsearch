'use client';

import Link from 'next/link';

import { useCrawlerBlueprintDetail, useCrawlerBlueprints } from '@/modules/admin/hooks/useCrawlerBlueprints';
import type {
  CrawlerBlueprintDetail,
  CrawlerBlueprintGenerationTask,
} from '@/modules/admin/types';
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
  if (['SUCCEEDED', 'SUCCESS', 'READY', 'ACTIVE'].includes(normalized)) return 'brand' as const;
  if (['FAILED'].includes(normalized)) return 'default' as const;
  if (['RUNNING', 'PENDING'].includes(normalized)) return 'brand' as const;
  return 'muted' as const;
}

function formatJson(value: unknown): string | null {
  if (value === null || value === undefined) {
    return null;
  }
  if (typeof value === 'string') {
    const trimmed = value.trim();
    if (!trimmed) {
      return null;
    }
    try {
      return JSON.stringify(JSON.parse(trimmed), null, 2);
    } catch (error) {
      return value;
    }
  }
  try {
    return JSON.stringify(value, null, 2);
  } catch (error) {
    return String(value);
  }
}

function JsonSection({ title, json }: { title: string; json: string | null }) {
  if (!json) {
    return null;
  }

  return (
    <div>
      <h4 className="text-xs font-semibold uppercase tracking-wide text-gray-500">{title}</h4>
      <pre className="mt-2 max-h-64 overflow-auto rounded-xl bg-gray-50 p-3 text-xs text-gray-700 shadow-inner">{json}</pre>
    </div>
  );
}

function TasksList({ tasks }: { tasks: CrawlerBlueprintGenerationTask[] }) {
  if (!tasks || tasks.length === 0) {
    return <p className="text-sm text-gray-600">暂无生成任务。</p>;
  }

  return (
    <div className="space-y-4">
      {tasks.map((task) => {
        const payloadJson = formatJson(task.inputPayload);
        const sampleJson = task.sampleData && task.sampleData.length ? formatJson(task.sampleData) : null;
        const snapshotJson = formatJson(task.browserSnapshot);
        const hasDetails = Boolean(payloadJson || sampleJson || snapshotJson);

        return (
          <div key={task.id} className="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <span className="font-medium text-gray-900">任务 #{task.id}</span>
                <Badge tone={statusTone(task.status)}>{task.status}</Badge>
              </div>
              <div className="text-xs text-gray-500">
                <span>开始：{formatDateTime(task.startedAt)}</span>
                <span className="mx-2 text-gray-300">·</span>
                <span>结束：{formatDateTime(task.finishedAt)}</span>
              </div>
            </div>

            {task.errorMessage && (
              <p className="mt-2 rounded-xl bg-rose-50 px-3 py-2 text-sm text-rose-700">
                ⚠️ 错误：{task.errorMessage}
              </p>
            )}

            {hasDetails && (
              <div className="mt-4 grid gap-4 lg:grid-cols-3">
                <JsonSection title="输入参数" json={payloadJson} />
                <JsonSection title="样例数据" json={sampleJson} />
                <JsonSection title="浏览器快照" json={snapshotJson} />
              </div>
            )}
          </div>
        );
      })}
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
      <div className="rounded-xl border border-rose-200 bg-rose-50 p-6">
        <p className="text-rose-800">{(detail.error as Error)?.message ?? '未找到该蓝图'}</p>
      </div>
    );
  }

  const blueprint: CrawlerBlueprintDetail = detail.data;
  const { summary, draftConfig, lastTestReport, recentTasks } = blueprint;
  const isRunning = rerun.isPending || activate.isPending;
  const prettyConfig = draftConfig ? formatJson(draftConfig) : null;
  const prettyReport = formatJson(lastTestReport);

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-2xl font-semibold text-gray-900">{summary.name}</h1>
            <Badge tone="muted">{summary.code}</Badge>
            {summary.status && <Badge tone={statusTone(summary.status)}>{summary.status}</Badge>}
            <Badge tone={summary.enabled ? 'brand' : 'default'}>{summary.enabled ? '已启用' : '已停用'}</Badge>
            {summary.autoGenerated != null && (
              <Badge tone={summary.autoGenerated ? 'brand' : 'muted'}>
                {summary.autoGenerated ? '自动生成' : '手动配置'}
              </Badge>
            )}
          </div>
          <div className="mt-3 flex flex-wrap gap-3 text-xs text-gray-500">
            <span>创建时间：{formatDateTime(summary.createdAt)}</span>
            <span>更新时间：{formatDateTime(summary.updatedAt)}</span>
            {summary.generatedAt && <span>最近生成：{formatDateTime(summary.generatedAt)}</span>}
            {summary.generatedBy && <span>操作人：{summary.generatedBy}</span>}
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
            onClick={() => activate.mutate({ code: summary.code, enabled: !summary.enabled })}
            disabled={isRunning}
          >
            {summary.enabled ? '暂停蓝图' : '启用蓝图'}
          </Button>
          <Button
            variant="primary"
            size="sm"
            onClick={() => rerun.mutate({ code: summary.code })}
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
            <dd className="mt-1 break-all text-gray-700">{summary.entryUrl}</dd>
          </div>
          <div>
            <dt className="font-medium text-gray-900">生成方式</dt>
            <dd className="mt-1 text-gray-700">
              {summary.autoGenerated == null ? '—' : summary.autoGenerated ? '自动生成' : '手动配置'}
            </dd>
          </div>
          <div>
            <dt className="font-medium text-gray-900">最近生成时间</dt>
            <dd className="mt-1 text-gray-700">{formatDateTime(summary.generatedAt)}</dd>
          </div>
          <div>
            <dt className="font-medium text-gray-900">最近生成操作者</dt>
            <dd className="mt-1 text-gray-700">{summary.generatedBy ?? '—'}</dd>
          </div>
        </dl>
      </section>

      <section className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-gray-900">最新生成配置</h2>
        {prettyConfig ? (
          <pre className="mt-4 max-h-96 overflow-auto rounded-xl bg-gray-50 p-4 text-xs text-gray-700 shadow-inner">{prettyConfig}</pre>
        ) : (
          <p className="mt-2 text-sm text-gray-600">尚未生成配置。</p>
        )}
      </section>

      <section className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-gray-900">最近测试报告</h2>
        {prettyReport ? (
          <pre className="mt-4 max-h-96 overflow-auto rounded-xl bg-gray-50 p-4 text-xs text-gray-700 shadow-inner">{prettyReport}</pre>
        ) : (
          <p className="mt-2 text-sm text-gray-600">暂无测试报告。</p>
        )}
      </section>

      <section className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-gray-900">最近生成任务</h2>
        <TasksList tasks={recentTasks ?? []} />
      </section>
    </div>
  );
}

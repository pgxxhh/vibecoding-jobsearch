'use client';

import { FormEvent, useMemo, useState } from 'react';
import Link from 'next/link';

import { useCrawlerBlueprints } from '@/modules/admin/hooks/useCrawlerBlueprints';
import type { CreateCrawlerBlueprintPayload } from '@/modules/admin/types';
import Badge from '@/shared/ui/Badge';
import Button from '@/shared/ui/Button';

const steps = ['基础信息', '配置 JSON', '确认创建'] as const;

type Step = 0 | 1 | 2;

type FormState = {
  code: string;
  name: string;
  entryUrl: string;
  parserTemplateCode: string;
  concurrencyLimit: string;
  description: string;
  tags: string;
  configJson: string;
};

const initialForm: FormState = {
  code: '',
  name: '',
  entryUrl: '',
  parserTemplateCode: '',
  concurrencyLimit: '4',
  description: '',
  tags: '',
  configJson: '{\n  "flow": [],\n  "parser": {}\n}',
};

export default function CrawlerBlueprintCreatePage() {
  const { create } = useCrawlerBlueprints();
  const [step, setStep] = useState<Step>(0);
  const [form, setForm] = useState<FormState>(initialForm);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const pending = create.isPending;

  const tagsArray = useMemo(
    () =>
      form.tags
        .split(',')
        .map((tag) => tag.trim())
        .filter(Boolean),
    [form.tags],
  );

  const isConfigValid = useMemo(() => {
    try {
      JSON.parse(form.configJson);
      return true;
    } catch {
      return false;
    }
  }, [form.configJson]);

  const parsedPayload = useMemo<CreateCrawlerBlueprintPayload>(() => {
    return {
      code: form.code.trim(),
      name: form.name.trim(),
      entryUrl: form.entryUrl.trim() || undefined,
      parserTemplateCode: form.parserTemplateCode.trim() || undefined,
      concurrencyLimit: form.concurrencyLimit ? Number(form.concurrencyLimit) : undefined,
      description: form.description.trim() || undefined,
      tags: tagsArray.length ? tagsArray : undefined,
      configJson: form.configJson,
    };
  }, [form, tagsArray]);

  const goNext = () => {
    setSuccess(null);
    if (step === 1 && !isConfigValid) {
      setError('配置 JSON 无法解析，请检查格式。');
      return;
    }
    setError(null);
    setStep((prev) => Math.min(2, (prev + 1) as Step));
  };

  const goPrev = () => {
    setError(null);
    setSuccess(null);
    setStep((prev) => Math.max(0, (prev - 1) as Step));
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setSuccess(null);

    if (!isConfigValid) {
      setError('配置 JSON 无法解析，请修正后再提交。');
      setStep(1);
      return;
    }

    if (!parsedPayload.code || !parsedPayload.name) {
      setError('Code 和名称为必填项。');
      setStep(0);
      return;
    }

    create.mutate(parsedPayload, {
      onSuccess: () => {
        setSuccess('蓝图创建成功，系统会在 5 秒内执行预热任务。');
        setError(null);
      },
      onError: (err) => {
        setSuccess(null);
        setError(err instanceof Error ? err.message : '创建失败');
      },
    });
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">新建爬虫蓝图</h1>
          <p className="mt-1 text-sm text-gray-600">按照向导填写基础信息与执行配置，提交后即可激活。</p>
        </div>
        <Link
          href="/admin/crawler-blueprints"
          className="inline-flex items-center justify-center gap-2 rounded-2xl border border-gray-200 px-4 py-2 text-sm text-gray-700 transition hover:border-brand-200 hover:text-brand-700"
        >
          ← 返回列表
        </Link>
      </div>

      <div className="flex flex-wrap gap-3">
        {steps.map((label, index) => (
          <div
            key={label}
            className={`flex items-center gap-2 rounded-2xl px-4 py-2 text-sm ${
              index === step ? 'bg-brand-50 text-brand-700 border border-brand-200' : 'bg-gray-50 text-gray-500 border border-gray-200'
            }`}
          >
            <span className="inline-flex h-6 w-6 items-center justify-center rounded-full bg-white text-xs font-semibold text-brand-600">
              {index + 1}
            </span>
            {label}
          </div>
        ))}
      </div>

      <form className="space-y-6" onSubmit={handleSubmit}>
        {step === 0 && (
          <div className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm space-y-4">
            <h2 className="text-lg font-semibold text-gray-900">① 基础信息</h2>
            <div className="grid gap-4 md:grid-cols-2">
              <label className="flex flex-col gap-2 text-sm">
                <span className="font-medium text-gray-700">蓝图 Code *</span>
                <input
                  className="rounded-xl border border-gray-200 bg-white px-4 py-3 text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
                  value={form.code}
                  onChange={(event) => setForm((prev) => ({ ...prev, code: event.target.value }))}
                  required
                  placeholder="jd-jobs"
                />
              </label>
              <label className="flex flex-col gap-2 text-sm">
                <span className="font-medium text-gray-700">蓝图名称 *</span>
                <input
                  className="rounded-xl border border-gray-200 bg-white px-4 py-3 text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
                  value={form.name}
                  onChange={(event) => setForm((prev) => ({ ...prev, name: event.target.value }))}
                  required
                  placeholder="京东 - 社招"
                />
              </label>
              <label className="flex flex-col gap-2 text-sm">
                <span className="font-medium text-gray-700">入口 URL</span>
                <input
                  className="rounded-xl border border-gray-200 bg-white px-4 py-3 text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
                  value={form.entryUrl}
                  onChange={(event) => setForm((prev) => ({ ...prev, entryUrl: event.target.value }))}
                  placeholder="https://careers.jd.com/jobs"
                />
              </label>
              <label className="flex flex-col gap-2 text-sm">
                <span className="font-medium text-gray-700">解析模板 Code</span>
                <input
                  className="rounded-xl border border-gray-200 bg-white px-4 py-3 text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
                  value={form.parserTemplateCode}
                  onChange={(event) => setForm((prev) => ({ ...prev, parserTemplateCode: event.target.value }))}
                  placeholder="jd-parser-v1"
                />
              </label>
              <label className="flex flex-col gap-2 text-sm">
                <span className="font-medium text-gray-700">并发限制</span>
                <input
                  type="number"
                  min={1}
                  max={32}
                  className="rounded-xl border border-gray-200 bg-white px-4 py-3 text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
                  value={form.concurrencyLimit}
                  onChange={(event) => setForm((prev) => ({ ...prev, concurrencyLimit: event.target.value }))}
                />
              </label>
              <label className="flex flex-col gap-2 text-sm md:col-span-2">
                <span className="font-medium text-gray-700">描述</span>
                <textarea
                  rows={3}
                  className="rounded-xl border border-gray-200 bg-white px-4 py-3 text-sm text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
                  value={form.description}
                  onChange={(event) => setForm((prev) => ({ ...prev, description: event.target.value }))}
                  placeholder="补充采集范围、目标公司等上下文，方便其他同学维护。"
                />
              </label>
              <label className="flex flex-col gap-2 text-sm md:col-span-2">
                <span className="font-medium text-gray-700">标签（逗号分隔）</span>
                <input
                  className="rounded-xl border border-gray-200 bg-white px-4 py-3 text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
                  value={form.tags}
                  onChange={(event) => setForm((prev) => ({ ...prev, tags: event.target.value }))}
                  placeholder="browser,自动化,playwright"
                />
              </label>
            </div>
          </div>
        )}

        {step === 1 && (
          <div className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm space-y-4">
            <h2 className="text-lg font-semibold text-gray-900">② 执行配置</h2>
            <p className="text-sm text-gray-600">
              粘贴 YAML/JSON 转换后的配置，至少包括 `flow`、`parser` 等字段。保存前系统会校验 JSON 格式。
            </p>
            <textarea
              rows={14}
              className="w-full rounded-2xl border border-gray-200 bg-white px-4 py-3 font-mono text-xs text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
              value={form.configJson}
              onChange={(event) => setForm((prev) => ({ ...prev, configJson: event.target.value }))}
            />
            {!isConfigValid && (
              <p className="text-xs text-rose-600">JSON 无法解析，请检查引号、逗号等格式。</p>
            )}
            {tagsArray.length > 0 && (
              <div className="flex flex-wrap gap-2 text-xs text-gray-600">
                <span className="font-medium text-gray-700">标签预览:</span>
                {tagsArray.map((tag) => (
                  <Badge key={tag} tone="muted">
                    #{tag}
                  </Badge>
                ))}
              </div>
            )}
          </div>
        )}

        {step === 2 && (
          <div className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm space-y-4">
            <h2 className="text-lg font-semibold text-gray-900">③ 确认信息</h2>
            <p className="text-sm text-gray-600">请再次确认以下字段，提交后后台会触发预热运行。</p>
            <dl className="grid gap-3 text-sm text-gray-700 md:grid-cols-2">
              <div>
                <dt className="font-medium text-gray-900">Code</dt>
                <dd className="mt-1 text-gray-700">{parsedPayload.code || '未填写'}</dd>
              </div>
              <div>
                <dt className="font-medium text-gray-900">名称</dt>
                <dd className="mt-1 text-gray-700">{parsedPayload.name || '未填写'}</dd>
              </div>
              <div>
                <dt className="font-medium text-gray-900">入口 URL</dt>
                <dd className="mt-1 break-all text-gray-700">{parsedPayload.entryUrl || '未填写'}</dd>
              </div>
              <div>
                <dt className="font-medium text-gray-900">解析模板</dt>
                <dd className="mt-1 text-gray-700">{parsedPayload.parserTemplateCode || '未填写'}</dd>
              </div>
              <div>
                <dt className="font-medium text-gray-900">并发限制</dt>
                <dd className="mt-1 text-gray-700">{parsedPayload.concurrencyLimit ?? '未填写'}</dd>
              </div>
              <div>
                <dt className="font-medium text-gray-900">标签</dt>
                <dd className="mt-1 text-gray-700">
                  {parsedPayload.tags?.length ? parsedPayload.tags.join(', ') : '未填写'}
                </dd>
              </div>
            </dl>
            <div>
              <dt className="font-medium text-gray-900">配置 JSON</dt>
              <pre className="mt-2 max-h-64 overflow-auto rounded-xl border border-gray-200 bg-gray-50 p-4 text-xs text-gray-700">
                {form.configJson}
              </pre>
            </div>
          </div>
        )}

        {error && (
          <div className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700">{error}</div>
        )}
        {success && (
          <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-700">{success}</div>
        )}

        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex gap-2">
            <Button variant="ghost" size="sm" onClick={goPrev} disabled={step === 0 || pending}>
              上一步
            </Button>
            {step < 2 && (
              <Button variant="outline" size="sm" onClick={goNext} disabled={pending}>
                下一步
              </Button>
            )}
          </div>
          <Button type="submit" variant="primary" size="md" disabled={pending}>
            {pending ? '创建中...' : '提交蓝图'}
          </Button>
        </div>
      </form>
    </div>
  );
}

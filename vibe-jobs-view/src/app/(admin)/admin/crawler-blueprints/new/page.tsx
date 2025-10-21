'use client';

import { FormEvent, useMemo, useState } from 'react';
import Link from 'next/link';

import { useCrawlerBlueprints } from '@/modules/admin/hooks/useCrawlerBlueprints';
import type { CreateCrawlerBlueprintPayload } from '@/modules/admin/types';
import Button from '@/shared/ui/Button';

const steps = ['基础信息', '高阶选项', '确认创建'] as const;

type Step = 0 | 1 | 2;

type FormState = {
  code: string;
  name: string;
  entryUrl: string;
  searchKeywords: string;
  excludeSelectors: string;
  notes: string;
};

const initialForm: FormState = {
  code: '',
  name: '',
  entryUrl: '',
  searchKeywords: '',
  excludeSelectors: '',
  notes: '',
};

function parseExcludeSelectors(raw: string): string[] {
  return raw
    .split(/[\n,]/)
    .map((value) => value.trim())
    .filter(Boolean);
}

export default function CrawlerBlueprintCreatePage() {
  const { create } = useCrawlerBlueprints();
  const [step, setStep] = useState<Step>(0);
  const [form, setForm] = useState<FormState>(initialForm);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const pending = create.isPending;

  const excludeSelectorList = useMemo(() => parseExcludeSelectors(form.excludeSelectors), [form.excludeSelectors]);

  const parsedPayload = useMemo<CreateCrawlerBlueprintPayload>(() => {
    const trimmedCode = form.code.trim();
    const trimmedName = form.name.trim();
    const trimmedEntryUrl = form.entryUrl.trim();
    const trimmedKeywords = form.searchKeywords.trim();
    const trimmedNotes = form.notes.trim();

    return {
      code: trimmedCode || undefined,
      name: trimmedName || undefined,
      entryUrl: trimmedEntryUrl,
      searchKeywords: trimmedKeywords || undefined,
      excludeSelectors: excludeSelectorList.length ? excludeSelectorList : undefined,
      notes: trimmedNotes || undefined,
    };
  }, [form.code, form.entryUrl, form.name, form.notes, form.searchKeywords, excludeSelectorList]);

  const goNext = () => {
    setSuccess(null);
    if (step === 0) {
      if (!form.code.trim() || !form.name.trim() || !form.entryUrl.trim()) {
        setError('请填写蓝图 Code、蓝图名称和入口 URL。');
        return;
      }
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

    if (!form.code.trim() || !form.name.trim() || !form.entryUrl.trim()) {
      setError('蓝图 Code、名称和入口 URL 为必填项。');
      setStep(0);
      return;
    }

    create.mutate(parsedPayload, {
      onSuccess: () => {
        setSuccess('蓝图创建成功，系统会立即触发自动生成任务。');
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
          <p className="mt-1 text-sm text-gray-600">填写基础信息后提交，后端会通过 Playwright 自动生成配置。</p>
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
              <label className="flex flex-col gap-2 text-sm md:col-span-2">
                <span className="font-medium text-gray-700">入口 URL *</span>
                <input
                  className="rounded-xl border border-gray-200 bg-white px-4 py-3 text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
                  value={form.entryUrl}
                  onChange={(event) => setForm((prev) => ({ ...prev, entryUrl: event.target.value }))}
                  required
                  placeholder="https://careers.jd.com/jobs"
                />
              </label>
              <label className="flex flex-col gap-2 text-sm md:col-span-2">
                <span className="font-medium text-gray-700">搜索关键词（可选）</span>
                <input
                  className="rounded-xl border border-gray-200 bg-white px-4 py-3 text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
                  value={form.searchKeywords}
                  onChange={(event) => setForm((prev) => ({ ...prev, searchKeywords: event.target.value }))}
                  placeholder="输入关键字后系统会自动执行一次搜索"
                />
              </label>
            </div>
          </div>
        )}

        {step === 1 && (
          <div className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm space-y-4">
            <h2 className="text-lg font-semibold text-gray-900">② 高阶选项</h2>
            <p className="text-sm text-gray-600">可选地提供需要排除的选择器与备注，便于自动化解析更准确。</p>
            <label className="flex flex-col gap-2 text-sm">
              <span className="font-medium text-gray-700">排除选择器（换行或逗号分隔）</span>
              <textarea
                className="min-h-[160px] rounded-2xl border border-gray-200 bg-gray-50 px-4 py-3 font-mono text-sm text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
                value={form.excludeSelectors}
                onChange={(event) => setForm((prev) => ({ ...prev, excludeSelectors: event.target.value }))}
                placeholder=".banner-ad\n.modal"
              />
            </label>
            <label className="flex flex-col gap-2 text-sm">
              <span className="font-medium text-gray-700">备注（例如登录方式、滚动要求等）</span>
              <textarea
                className="min-h-[120px] rounded-2xl border border-gray-200 bg-white px-4 py-3 text-sm text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
                value={form.notes}
                onChange={(event) => setForm((prev) => ({ ...prev, notes: event.target.value }))}
                placeholder="抓取前需要点击“同意 Cookies”弹窗。"
              />
            </label>
          </div>
        )}

        {step === 2 && (
          <div className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm space-y-4">
            <h2 className="text-lg font-semibold text-gray-900">③ 确认创建</h2>
            <p className="text-sm text-gray-600">确认以下信息无误后提交，系统会触发 Playwright 自动生成蓝图。</p>
            <dl className="grid gap-3 text-sm text-gray-700 md:grid-cols-2">
              <div>
                <dt className="font-medium text-gray-900">蓝图 Code</dt>
                <dd className="mt-1 text-gray-700">{form.code.trim() || '—'}</dd>
              </div>
              <div>
                <dt className="font-medium text-gray-900">蓝图名称</dt>
                <dd className="mt-1 text-gray-700">{form.name.trim() || '—'}</dd>
              </div>
              <div className="md:col-span-2">
                <dt className="font-medium text-gray-900">入口 URL</dt>
                <dd className="mt-1 break-all text-gray-700">{form.entryUrl.trim() || '—'}</dd>
              </div>
              <div className="md:col-span-2">
                <dt className="font-medium text-gray-900">搜索关键词</dt>
                <dd className="mt-1 text-gray-700">{form.searchKeywords.trim() || '—'}</dd>
              </div>
              <div className="md:col-span-2">
                <dt className="font-medium text-gray-900">排除选择器</dt>
                <dd className="mt-1 text-gray-700">
                  {excludeSelectorList.length ? (
                    <ul className="space-y-1 whitespace-pre-line">
                      {excludeSelectorList.map((selector) => (
                        <li key={selector}>• {selector}</li>
                      ))}
                    </ul>
                  ) : (
                    '—'
                  )}
                </dd>
              </div>
              <div className="md:col-span-2">
                <dt className="font-medium text-gray-900">备注</dt>
                <dd className="mt-1 whitespace-pre-line text-gray-700">{form.notes.trim() || '—'}</dd>
              </div>
            </dl>
          </div>
        )}

        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="text-sm text-gray-600">
            {error && <p className="text-rose-600">{error}</p>}
            {success && <p className="text-brand-700">{success}</p>}
          </div>
          <div className="flex items-center gap-3">
            {step > 0 && (
              <Button type="button" variant="ghost" onClick={goPrev} disabled={pending}>
                上一步
              </Button>
            )}
            {step < 2 && (
              <Button type="button" onClick={goNext} disabled={pending}>
                下一步
              </Button>
            )}
            {step === 2 && (
              <Button type="submit" variant="primary" disabled={pending}>
                {pending ? '提交中...' : '确认创建'}
              </Button>
            )}
          </div>
        </div>
      </form>
    </div>
  );
}

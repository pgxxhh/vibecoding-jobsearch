'use client';

import { useState, useRef, FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { bulkUploadDataSources } from '@/modules/admin/services/dataSourcesService';
import type { DataSourcePayload } from '@/modules/admin/types';

interface BulkUploadModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function DataSourceBulkUpload({ isOpen, onClose }: BulkUploadModalProps) {
  const queryClient = useQueryClient();
  const [uploadData, setUploadData] = useState('');
  const [message, setMessage] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const uploadMutation = useMutation({
    mutationFn: bulkUploadDataSources,
    onSuccess: async (result) => {
      await queryClient.invalidateQueries({ queryKey: ['admin', 'data-sources'] });
      setMessage(`成功上传 ${result.success || 0} 个数据源，失败 ${result.failed || 0} 个`);
      setErrorMsg(null);
      setUploadData('');
    },
    onError: (err: unknown) => {
      setMessage(null);
      setErrorMsg(err instanceof Error ? err.message : '上传失败');
    },
  });

  const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (e) => {
      const content = e.target?.result as string;
      setUploadData(content);
    };
    reader.readAsText(file);
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    try {
      if (!uploadData.trim()) {
        throw new Error('请输入数据或选择文件');
      }

      const parsed = JSON.parse(uploadData);
      let dataSources: DataSourcePayload[];

      if (Array.isArray(parsed)) {
        dataSources = parsed;
      } else {
        dataSources = [parsed];
      }

      // 验证数据格式
      for (const ds of dataSources) {
        if (!ds.code || !ds.type) {
          throw new Error(`数据格式错误：缺少必要字段 code 或 type`);
        }
      }

      uploadMutation.mutate(dataSources);
    } catch (err) {
      setMessage(null);
      setErrorMsg(err instanceof Error ? err.message : '数据格式错误');
    }
  };

  const exampleData = {
    code: 'example-company',
    type: 'lever',
    enabled: true,
    runOnStartup: false,
    requireOverride: false,
    flow: 'UNLIMITED',
    baseOptions: {},
    categories: [],
    companies: [
      {
        reference: 'example-corp',
        displayName: 'Example Corporation',
        slug: 'example',
        enabled: true,
        placeholderOverrides: {},
        overrideOptions: {}
      }
    ]
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-4xl rounded-3xl border border-white/60 bg-white/95 p-6 shadow-brand-lg backdrop-blur-sm">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-xl font-semibold text-gray-900">📤 批量上传数据源</h3>
          <button
            onClick={onClose}
            className="inline-flex items-center justify-center h-8 w-8 rounded-full text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition"
          >
            ✕
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label className="flex flex-col space-y-2 text-sm">
              <span className="font-medium text-gray-700">上传文件（JSON格式）</span>
              <input
                ref={fileInputRef}
                type="file"
                accept=".json,.txt"
                onChange={handleFileUpload}
                className="rounded-xl border border-gray-200 bg-white px-4 py-3 text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
              />
            </label>
          </div>

          <div>
            <label className="flex flex-col space-y-2 text-sm">
              <span className="font-medium text-gray-700">或直接粘贴JSON数据</span>
              <textarea
                value={uploadData}
                onChange={(e) => setUploadData(e.target.value)}
                rows={12}
                className="rounded-xl border border-gray-200 bg-white px-4 py-3 font-mono text-xs text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15"
                placeholder="支持单个对象或对象数组格式"
              />
            </label>
          </div>

          <div className="rounded-2xl border border-gray-200 bg-gray-50 p-4">
            <details className="cursor-pointer">
              <summary className="text-sm font-medium text-gray-700 mb-3 select-none">
                📋 数据格式示例（点击展开）
              </summary>
              <pre className="text-xs text-gray-600 overflow-x-auto bg-white rounded-xl p-4 border border-gray-200">
                {JSON.stringify(exampleData, null, 2)}
              </pre>
            </details>
          </div>

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

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-gray-200">
            <button
              type="button"
              onClick={onClose}
              className="inline-flex items-center justify-center gap-2 rounded-2xl transition active:scale-[.98] h-10 px-6 text-sm border border-gray-300 bg-white text-gray-700 hover:bg-gray-50 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-gray-500/15"
            >
              取消
            </button>
            <button
              type="submit"
              disabled={uploadMutation.isPending}
              className="inline-flex items-center justify-center gap-2 rounded-2xl transition active:scale-[.98] disabled:cursor-not-allowed disabled:opacity-60 h-10 px-6 text-sm bg-brand-600 text-white hover:bg-brand-700 shadow-brand-sm focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-500/30"
            >
              {uploadMutation.isPending ? '上传中...' : '🚀 开始上传'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
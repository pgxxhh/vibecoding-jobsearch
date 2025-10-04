'use client';

import { useState, useRef, FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';

interface DataSourcePayload {
  code: string;
  type: string;
  enabled: boolean;
  runOnStartup: boolean;
  requireOverride: boolean;
  flow: 'LIMITED' | 'UNLIMITED';
  baseOptions: Record<string, any>;
  categories: any[];
  companies: any[];
}

interface BulkUploadModalProps {
  isOpen: boolean;
  onClose: () => void;
}

async function uploadBulkDataSources(dataSources: DataSourcePayload[]): Promise<any> {
  const res = await fetch('/api/admin/data-sources/bulk', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ dataSources }),
  });
  
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || '批量上传失败');
  }
  
  return res.json();
}

export default function DataSourceBulkUpload({ isOpen, onClose }: BulkUploadModalProps) {
  const queryClient = useQueryClient();
  const [uploadData, setUploadData] = useState('');
  const [message, setMessage] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const uploadMutation = useMutation({
    mutationFn: uploadBulkDataSources,
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
      <div className="w-full max-w-4xl rounded-xl border border-white/10 bg-gray-900 p-6">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-xl font-semibold text-white">批量上传数据源</h3>
          <button
            onClick={onClose}
            className="text-white/60 hover:text-white/80"
          >
            ✕
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="flex flex-col space-y-2 text-sm text-white/80">
              <span>上传文件（JSON格式）</span>
              <input
                ref={fileInputRef}
                type="file"
                accept=".json,.txt"
                onChange={handleFileUpload}
                className="rounded-md border border-white/10 bg-white/5 px-3 py-2 text-white focus:border-white/40 focus:outline-none"
              />
            </label>
          </div>

          <div>
            <label className="flex flex-col space-y-2 text-sm text-white/80">
              <span>或直接粘贴JSON数据</span>
              <textarea
                value={uploadData}
                onChange={(e) => setUploadData(e.target.value)}
                rows={12}
                className="rounded-md border border-white/10 bg-white/5 px-3 py-2 font-mono text-xs text-white focus:border-white/40 focus:outline-none"
                placeholder="支持单个对象或对象数组格式"
              />
            </label>
          </div>

          <div className="rounded-lg border border-white/10 bg-white/5 p-3">
            <details className="cursor-pointer">
              <summary className="text-sm font-medium text-white/80 mb-2">
                数据格式示例（点击展开）
              </summary>
              <pre className="text-xs text-white/60 overflow-x-auto">
                {JSON.stringify(exampleData, null, 2)}
              </pre>
            </details>
          </div>

          {message && <p className="text-sm text-emerald-300">{message}</p>}
          {errorMsg && <p className="text-sm text-rose-300">{errorMsg}</p>}

          <div className="flex items-center justify-end space-x-3">
            <button
              type="button"
              onClick={onClose}
              className="rounded-md bg-white/5 px-4 py-2 text-sm font-medium text-white/70 hover:bg-white/10"
            >
              取消
            </button>
            <button
              type="submit"
              disabled={uploadMutation.isPending}
              className="rounded-md bg-white/10 px-4 py-2 text-sm font-medium text-white hover:bg-white/20 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {uploadMutation.isPending ? '上传中...' : '开始上传'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
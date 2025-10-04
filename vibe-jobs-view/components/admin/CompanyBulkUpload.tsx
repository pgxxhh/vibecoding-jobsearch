'use client';

import { FormEvent, useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';

interface CompanyData {
  reference: string;
  displayName?: string;
  slug?: string;
  enabled?: boolean;
  placeholderOverrides?: Record<string, string>;
  overrideOptions?: Record<string, string>;
}

interface BulkCompanyUploadProps {
  isOpen: boolean;
  onClose: () => void;
  dataSourceCode: string;
}

export default function CompanyBulkUpload({ isOpen, onClose, dataSourceCode }: BulkCompanyUploadProps) {
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [uploadData, setUploadData] = useState('');
  const [message, setMessage] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Debug: Log when component mounts/updates
  // console.log('CompanyBulkUpload render:', { isOpen, dataSourceCode });

  const handleClose = () => {
    setMessage(null);
    setErrorMsg(null);
    setUploadData('');
    onClose();
  };

  const uploadMutation = useMutation({
    mutationFn: async (companies: CompanyData[]) => {
      const res = await fetch(`/api/admin/data-sources/${dataSourceCode}/companies/bulk`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ companies }),
      });
      if (!res.ok) {
        const text = await res.text();
        throw new Error(text || 'Upload failed');
      }
      return res.json();
    },
    onSuccess: async (result) => {
      // Invalidate both paged and non-paged queries
      await queryClient.invalidateQueries({ queryKey: ['admin', 'data-source', dataSourceCode] });
      await queryClient.invalidateQueries({ queryKey: ['admin', 'data-source-paged', dataSourceCode] });
      
      if (result.failed > 0) {
        setMessage(`成功创建 ${result.successful} 个公司，${result.failed} 个失败`);
        setErrorMsg(result.errors.join('; '));
      } else {
        setMessage(`成功批量创建了 ${result.successful} 个公司`);
        setErrorMsg(null);
      }
      
      // Clear form after successful upload
      setUploadData('');
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    },
    onError: (error: unknown) => {
      setMessage(null);
      setErrorMsg(error instanceof Error ? error.message : '上传失败');
    },
  });

  const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (e) => {
      const content = e.target?.result as string;
      setUploadData(content);
      setMessage(null);
      setErrorMsg(null);
    };
    reader.onerror = () => {
      setErrorMsg('文件读取失败');
    };
    reader.readAsText(file);
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    
    if (!uploadData.trim()) {
      setErrorMsg('请输入公司数据或上传文件');
      return;
    }

    try {
      // Parse JSON data
      let companies: CompanyData[];
      const parsed = JSON.parse(uploadData.trim());
      
      // Handle both single object and array
      if (Array.isArray(parsed)) {
        companies = parsed;
      } else {
        companies = [parsed];
      }

      // Validate each company
      for (let i = 0; i < companies.length; i++) {
        const company = companies[i];
        if (!company.reference || typeof company.reference !== 'string') {
          throw new Error(`公司 #${i + 1} 缺少必需的 reference 字段`);
        }
      }

      setMessage(null);
      setErrorMsg(null);
      uploadMutation.mutate(companies);
      
    } catch (error) {
      setMessage(null);
      setErrorMsg(error instanceof Error ? error.message : '数据格式错误');
    }
  };

  const exampleData = [
    {
      reference: "google",
      displayName: "Google",
      slug: "google",
      enabled: true,
      placeholderOverrides: {
        "location": "Mountain View, CA"
      },
      overrideOptions: {
        "department": "Engineering"
      }
    },
    {
      reference: "microsoft",
      displayName: "Microsoft Corporation",
      slug: "microsoft",
      enabled: true
    }
  ];

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/50">
      <div 
        className="fixed inset-0" 
        onClick={handleClose}
      />
      <div 
        className="relative w-full max-w-4xl rounded-3xl border border-gray-200 bg-white p-6 shadow-2xl z-10"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-xl font-semibold text-gray-900">🏢 批量上传公司</h3>
          <button
            type="button"
            onClick={handleClose}
            className="inline-flex items-center justify-center h-8 w-8 rounded-full text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
          >
            <span className="text-lg">✕</span>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label className="flex flex-col space-y-2 text-sm">
              <span className="font-medium text-gray-700">上传JSON文件</span>
              <input
                ref={fileInputRef}
                type="file"
                accept=".json,.txt"
                onChange={handleFileUpload}
                className="rounded-xl border border-gray-200 bg-white px-4 py-3 text-gray-900 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
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
                className="rounded-xl border border-gray-200 bg-white px-4 py-3 font-mono text-xs text-gray-900 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                placeholder="支持单个公司对象或公司数组格式"
              />
            </label>
          </div>

          <div className="rounded-2xl border border-gray-200 bg-gray-50 p-4">
            <details className="cursor-pointer">
              <summary className="text-sm font-medium text-gray-700 mb-3 select-none hover:text-gray-900">
                📋 公司数据格式示例（点击展开）
              </summary>
              <div className="space-y-2">
                <p className="text-xs text-gray-600">必需字段：</p>
                <ul className="text-xs text-gray-600 ml-4 space-y-1">
                  <li>• <code className="bg-gray-100 px-1 rounded">reference</code> - 公司唯一标识符</li>
                </ul>
                <p className="text-xs text-gray-600 mt-3">可选字段：</p>
                <ul className="text-xs text-gray-600 ml-4 space-y-1">
                  <li>• <code className="bg-gray-100 px-1 rounded">displayName</code> - 显示名称</li>
                  <li>• <code className="bg-gray-100 px-1 rounded">slug</code> - URL友好标识</li>
                  <li>• <code className="bg-gray-100 px-1 rounded">enabled</code> - 是否启用 (默认: true)</li>
                  <li>• <code className="bg-gray-100 px-1 rounded">placeholderOverrides</code> - 占位符覆盖配置</li>
                  <li>• <code className="bg-gray-100 px-1 rounded">overrideOptions</code> - 选项覆盖配置</li>
                </ul>
                <pre className="text-xs text-gray-600 overflow-x-auto bg-white rounded-xl p-4 border border-gray-200 mt-3">
                  {JSON.stringify(exampleData, null, 2)}
                </pre>
              </div>
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
              onClick={handleClose}
              className="inline-flex items-center justify-center gap-2 rounded-2xl transition active:scale-[.98] h-10 px-6 text-sm border border-gray-300 bg-white text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-500/20"
            >
              取消
            </button>
            <button
              type="submit"
              disabled={uploadMutation.isPending}
              className="inline-flex items-center justify-center gap-2 rounded-2xl transition active:scale-[.98] disabled:cursor-not-allowed disabled:opacity-60 h-10 px-6 text-sm bg-blue-600 text-white hover:bg-blue-700 shadow-lg focus:outline-none focus:ring-2 focus:ring-blue-500/30"
            >
              {uploadMutation.isPending ? '上传中...' : '🚀 开始上传'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
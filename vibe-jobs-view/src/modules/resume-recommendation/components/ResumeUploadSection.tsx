'use client';

import { useRef, useState } from 'react';
import { Button, Card, Input } from '@/shared/ui';

interface Props {
  onUpload: (file: File, userId?: string) => Promise<void>;
  isLoading: boolean;
  parseStatus: string | null;
  error?: string | null;
}

export function ResumeUploadSection({ onUpload, isLoading, parseStatus, error }: Props) {
  const fileRef = useRef<HTMLInputElement | null>(null);
  const [userId, setUserId] = useState('');
  const [fileName, setFileName] = useState('');

  const handleSubmit = async () => {
    const file = fileRef.current?.files?.[0];
    if (!file) return;
    await onUpload(file, userId.trim() || undefined);
    setFileName(file.name);
  };

  return (
    <Card className="space-y-4 border-white/60 bg-white/95 p-4 shadow-brand-lg">
      <div className="space-y-2">
        <h2 className="text-lg font-semibold text-slate-900">上传简历</h2>
        <p className="text-sm text-gray-600">支持 PDF、Word、TXT，上传后自动生成匹配岗位。</p>
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        <label className="text-sm font-medium text-gray-700">选择文件</label>
        <input
          ref={fileRef}
          type="file"
          accept=".pdf,.doc,.docx,.txt"
          className="text-sm"
          aria-label="resume file"
        />
        <label className="text-sm font-medium text-gray-700">用户ID（可选）</label>
        <Input
          value={userId}
          onChange={(event) => setUserId(event.target.value)}
          placeholder="用于标记上传者"
        />
      </div>
      <div className="flex flex-wrap items-center gap-3">
        <Button onClick={handleSubmit} disabled={isLoading}>
          {isLoading ? '上传中...' : '上传并获取推荐'}
        </Button>
        {fileName && <span className="text-sm text-gray-600">已选择：{fileName}</span>}
        {parseStatus && <span className="text-sm text-gray-700">解析状态：{parseStatus}</span>}
        {error && <span className="text-sm text-red-600">{error}</span>}
      </div>
    </Card>
  );
}

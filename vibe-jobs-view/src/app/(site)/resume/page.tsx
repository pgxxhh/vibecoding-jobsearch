'use client';

import { useState } from 'react';
import { RecommendationList } from '@/modules/resume-recommendation/components/RecommendationList';
import { ResumeUploadSection } from '@/modules/resume-recommendation/components/ResumeUploadSection';
import { useResumeRecommendations } from '@/modules/resume-recommendation/hooks/useResumeRecommendations';
import type { RecommendationFilters } from '@/modules/resume-recommendation/types';
import { Card, Input, Button } from '@/shared/ui';

export default function ResumeRecommendationsPage() {
  const { state, upload, loadRecommendations, sendFeedback } = useResumeRecommendations();
  const [filters, setFilters] = useState<RecommendationFilters>({ location: '', limit: 10 });

  const handleUpload = async (file: File, userId?: string) => {
    const resumeId = await upload(file, userId);
    await loadRecommendations(resumeId, filters);
  };

  const handleFeedback = async (jobId: number, feedback: 'LIKE' | 'DISLIKE') => {
    if (!state.resumeId) return;
    await sendFeedback(state.resumeId, [{ jobId, feedback }]);
  };

  const handleRefresh = async () => {
    if (!state.resumeId) return;
    await loadRecommendations(state.resumeId, filters);
  };

  return (
    <main className="mx-auto flex max-w-5xl flex-col gap-6 px-4 py-10">
      <div className="space-y-2">
        <h1 className="text-3xl font-semibold text-slate-900">简历推荐</h1>
        <p className="text-sm text-gray-600">上传简历后，系统会结合技能与岗位文本给出匹配岗位并提供解释。</p>
      </div>
      <ResumeUploadSection
        onUpload={handleUpload}
        isLoading={state.isLoading}
        parseStatus={state.parseStatus}
        error={state.error}
      />
      <Card className="space-y-3 border-white/60 bg-white/95 p-4 shadow-brand-lg">
        <div className="flex flex-wrap items-center gap-3">
          <Input
            value={filters.location ?? ''}
            onChange={(event) => setFilters((prev) => ({ ...prev, location: event.target.value }))}
            placeholder="地点筛选"
          />
          <Button variant="outline" onClick={handleRefresh} disabled={!state.resumeId || state.isLoading}>
            重新获取推荐
          </Button>
        </div>
      </Card>
      <RecommendationList items={state.recommendations} onFeedback={handleFeedback} />
    </main>
  );
}

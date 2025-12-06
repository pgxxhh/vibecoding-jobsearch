'use client';

import { useState } from 'react';
import type { RecommendationItem, FeedbackValue } from '@/modules/resume-recommendation/types';
import { Button, Card, Badge } from '@/shared/ui';

interface Props {
  items: RecommendationItem[];
  onFeedback: (jobId: number, feedback: FeedbackValue) => Promise<void>;
}

export function RecommendationList({ items, onFeedback }: Props) {
  const [submittingId, setSubmittingId] = useState<number | null>(null);

  const handleFeedback = async (jobId: number, feedback: FeedbackValue) => {
    setSubmittingId(jobId);
    await onFeedback(jobId, feedback);
    setSubmittingId(null);
  };

  if (!items.length) {
    return <p className="text-sm text-gray-600">暂无推荐，请先上传简历。</p>;
  }

  return (
    <div className="space-y-4">
      {items.map((item) => (
        <Card key={item.jobId} className="space-y-3 border-white/60 bg-white/95 p-4 shadow-brand-lg">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h3 className="text-lg font-semibold text-slate-900">{item.title}</h3>
              <p className="text-sm text-gray-700">{item.company}</p>
              <p className="text-sm text-gray-500">{item.location}</p>
            </div>
            <Badge tone="brand">匹配分 {item.score.toFixed(2)}</Badge>
          </div>
          <p className="text-sm text-gray-700">{item.explanation}</p>
          {item.skillHits.length > 0 && (
            <div className="flex flex-wrap gap-2 text-xs text-gray-600">
              {item.skillHits.map((skill) => (
                <Badge key={skill} tone="neutral">
                  {skill}
                </Badge>
              ))}
            </div>
          )}
          <div className="flex gap-3">
            <Button
              variant="outline"
              disabled={submittingId === item.jobId}
              onClick={() => handleFeedback(item.jobId, 'LIKE')}
            >
              👍 合适
            </Button>
            <Button
              variant="ghost"
              disabled={submittingId === item.jobId}
              onClick={() => handleFeedback(item.jobId, 'DISLIKE')}
            >
              👎 不相关
            </Button>
          </div>
        </Card>
      ))}
    </div>
  );
}

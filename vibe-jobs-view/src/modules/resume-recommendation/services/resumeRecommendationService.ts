import { joinApiPath } from '@/shared/lib/api-base';
import type {
  FeedbackItem,
  RecommendationFilters,
  RecommendationResponse,
  ResumeUploadResult,
} from '@/modules/resume-recommendation/types';

function buildQuery(params: RecommendationFilters): string {
  const entries = Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .map(([key, value]) => [key, String(value)] as [string, string]);
  return new URLSearchParams(entries).toString();
}

export async function uploadResume(file: File, userId?: string): Promise<ResumeUploadResult> {
  const formData = new FormData();
  formData.append('file', file);
  if (userId) {
    formData.append('userId', userId);
  }

  const response = await fetch(joinApiPath('resumes/upload'), {
    method: 'POST',
    body: formData,
  });
  if (!response.ok) {
    throw new Error('Failed to upload resume');
  }
  return (await response.json()) as ResumeUploadResult;
}

export async function fetchRecommendations(
  resumeId: number,
  filters: RecommendationFilters = {},
): Promise<RecommendationResponse> {
  const qs = buildQuery(filters);
  const response = await fetch(
    qs ? `${joinApiPath(`resumes/${resumeId}/recommendations`)}?${qs}` : joinApiPath(`resumes/${resumeId}/recommendations`),
    { cache: 'no-store' },
  );
  if (!response.ok) {
    throw new Error('Failed to fetch recommendations');
  }
  return (await response.json()) as RecommendationResponse;
}

export async function submitFeedback(resumeId: number, items: FeedbackItem[]): Promise<void> {
  const response = await fetch(joinApiPath(`resumes/${resumeId}/feedback`), {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ items }),
  });
  if (!response.ok) {
    throw new Error('Failed to submit feedback');
  }
}

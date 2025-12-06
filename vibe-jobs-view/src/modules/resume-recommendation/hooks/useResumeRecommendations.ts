import { useCallback, useState } from 'react';
import type {
  FeedbackItem,
  RecommendationFilters,
  RecommendationItem,
  RecommendationResponse,
} from '@/modules/resume-recommendation/types';
import { fetchRecommendations, submitFeedback, uploadResume } from '@/modules/resume-recommendation/services/resumeRecommendationService';

export interface UseResumeRecommendationsState {
  resumeId: number | null;
  parseStatus: string | null;
  recommendations: RecommendationItem[];
  isLoading: boolean;
  error: string | null;
}

export function useResumeRecommendations() {
  const [state, setState] = useState<UseResumeRecommendationsState>({
    resumeId: null,
    parseStatus: null,
    recommendations: [],
    isLoading: false,
    error: null,
  });

  const upload = useCallback(async (file: File, userId?: string) => {
    setState((prev) => ({ ...prev, isLoading: true, error: null }));
    try {
      const uploaded = await uploadResume(file, userId);
      setState((prev) => ({
        ...prev,
        resumeId: uploaded.resumeId,
        parseStatus: uploaded.parseStatus,
      }));
      return uploaded.resumeId;
    } catch (err) {
      setState((prev) => ({ ...prev, error: err instanceof Error ? err.message : 'Upload failed' }));
      throw err;
    } finally {
      setState((prev) => ({ ...prev, isLoading: false }));
    }
  }, []);

  const loadRecommendations = useCallback(async (resumeId: number, filters: RecommendationFilters = {}) => {
    setState((prev) => ({ ...prev, isLoading: true, error: null }));
    try {
      const response: RecommendationResponse = await fetchRecommendations(resumeId, filters);
      setState((prev) => ({ ...prev, recommendations: response.items }));
    } catch (err) {
      setState((prev) => ({ ...prev, error: err instanceof Error ? err.message : 'Failed to load' }));
    } finally {
      setState((prev) => ({ ...prev, isLoading: false }));
    }
  }, []);

  const sendFeedback = useCallback(async (resumeId: number, items: FeedbackItem[]) => {
    await submitFeedback(resumeId, items);
  }, []);

  const reset = useCallback(() => {
    setState({ resumeId: null, parseStatus: null, recommendations: [], isLoading: false, error: null });
  }, []);

  return {
    state,
    upload,
    loadRecommendations,
    sendFeedback,
    reset,
  };
}

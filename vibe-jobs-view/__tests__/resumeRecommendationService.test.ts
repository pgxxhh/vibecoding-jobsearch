import { fetchRecommendations, submitFeedback, uploadResume } from '@/modules/resume-recommendation/services/resumeRecommendationService';
import type { FeedbackItem } from '@/modules/resume-recommendation/types';

describe('resumeRecommendationService', () => {
  beforeEach(() => {
    // @ts-expect-error
    global.fetch = jest.fn();
  });

  it('posts resume file via FormData', async () => {
    const mockResponse = { resumeId: 10, parseStatus: 'READY' };
    // @ts-expect-error
    global.fetch.mockResolvedValue({ ok: true, json: async () => mockResponse });
    const file = new File(['hello'], 'resume.txt', { type: 'text/plain' });
    const result = await uploadResume(file, '99');
    expect(result).toEqual(mockResponse);
    expect(global.fetch).toHaveBeenCalledWith(expect.stringContaining('/api/resumes/upload'), expect.any(Object));
  });

  it('fetches recommendations with filters', async () => {
    // @ts-expect-error
    global.fetch.mockResolvedValue({ ok: true, json: async () => ({ items: [] }) });
    await fetchRecommendations(5, { location: 'remote', limit: 5 });
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/resumes/5/recommendations?location=remote&limit=5'),
      expect.objectContaining({ cache: 'no-store' }),
    );
  });

  it('submits feedback as json body', async () => {
    // @ts-expect-error
    global.fetch.mockResolvedValue({ ok: true });
    const items: FeedbackItem[] = [{ jobId: 1, feedback: 'LIKE' }];
    await submitFeedback(3, items);
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/resumes/3/feedback'),
      expect.objectContaining({ method: 'POST' }),
    );
  });
});

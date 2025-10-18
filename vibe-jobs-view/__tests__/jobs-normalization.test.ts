import { normalizeJobDetailFromApi, normalizeJobFromApi } from '@/modules/job-search/utils/jobs-normalization';

const baseJobPayload = {
  id: 'job-1',
  title: 'Software Engineer',
  company: 'Acme Corp',
  location: 'Remote',
  postedAt: '2024-01-01T00:00:00Z',
};

describe('normalizeJobFromApi', () => {
  it('prefers the top-level enrichmentStatus when present', () => {
    const result = normalizeJobFromApi({
      ...baseJobPayload,
      summary: 'Original summary',
      enrichments: {
        summary: 'Enriched summary',
      },
      enrichmentStatus: {
        state: 'success',
        message: 'all good',
      },
    });

    expect(result.enrichmentStatus).toEqual({ state: 'success', message: 'all good' });
    expect(result.summary).toBe('Enriched summary');
  });

  it('falls back to enrichment status nested inside enrichments', () => {
    const result = normalizeJobFromApi({
      ...baseJobPayload,
      id: 'job-2',
      enrichments: {
        status: {
          state: 'SUCCESS',
          message: 'done',
        },
        summary: 'Nested summary',
        skills: ['TypeScript'],
      },
    });

    expect(result.enrichmentStatus).toEqual({ state: 'SUCCESS', message: 'done' });
    expect(result.summary).toBe('Nested summary');
    expect(result.skills).toEqual(['TypeScript']);
  });
});

describe('normalizeJobDetailFromApi', () => {
  it('returns nested enrichment status when top-level value is missing', () => {
    const detail = normalizeJobDetailFromApi({
      ...baseJobPayload,
      enrichments: {
        status: {
          state: 'success',
        },
        summary: 'Detail summary',
        skills: ['React'],
      },
    }, 'job-2');

    expect(detail.enrichmentStatus).toEqual({ state: 'success' });
    expect(detail.summary).toBe('Detail summary');
    expect(detail.skills).toEqual(['React']);
  });
});

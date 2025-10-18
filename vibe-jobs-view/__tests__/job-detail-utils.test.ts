import { resolveEnrichmentStatus } from '@/modules/job-search/components/JobDetail';
import type { Job } from '@/modules/job-search/types';

function createJob(overrides: Partial<Job> = {}): Job {
  return {
    id: 'job-1',
    title: 'Software Engineer',
    company: 'Acme Corp',
    location: 'Remote',
    postedAt: '2024-01-01T00:00:00Z',
    url: 'https://example.com',
    ...overrides,
  };
}

describe('resolveEnrichmentStatus', () => {
  it('returns the top-level enrichmentStatus when available', () => {
    const job = createJob({ enrichmentStatus: { state: 'success' } });

    expect(resolveEnrichmentStatus(job)).toEqual({ state: 'success' });
  });

  it('falls back to enrichment status nested under enrichments', () => {
    const job = createJob({
      enrichments: {
        status: {
          state: 'SUCCESS',
          message: 'done',
        },
      },
    });

    expect(resolveEnrichmentStatus(job)).toEqual({ state: 'SUCCESS', message: 'done' });
  });

  it('returns undefined when no status can be found', () => {
    const job = createJob();

    expect(resolveEnrichmentStatus(job)).toBeUndefined();
  });
});

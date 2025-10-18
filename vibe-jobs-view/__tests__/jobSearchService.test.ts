import { fetchJobDetail, fetchJobs } from '@/modules/job-search/services/jobSearchService';

const originalFetch = global.fetch;

describe('jobSearchService', () => {
  beforeEach(() => {
    global.fetch = jest.fn();
  });

  afterEach(() => {
    jest.resetAllMocks();
  });

  afterAll(() => {
    global.fetch = originalFetch;
  });

  it('builds query params and normalizes job responses', async () => {
    const response = {
      ok: true,
      json: async () => ({
        items: [
          {
            id: '1',
            title: 'Engineer',
            company: 'Acme',
            location: 'Remote',
            postedAt: '2024-01-01T00:00:00Z',
            url: 'https://example.com/jobs/1',
            enrichments: { summary: 'Enriched summary' },
            enrichmentStatus: { state: 'SUCCESS' },
          },
        ],
        total: '5',
        nextCursor: 'cursor-1',
        hasMore: true,
        size: 25,
      }),
    } as unknown as Response;

    (global.fetch as jest.Mock).mockResolvedValue(response);

    const result = await fetchJobs({ q: 'react', location: 'remote', size: 10, cursor: null });

    expect(global.fetch).toHaveBeenCalledWith('/api/jobs?q=react&location=remote&size=10', { cache: 'no-store' });
    expect(result.items).toHaveLength(1);
    expect(result.items[0]).toMatchObject({ id: '1', summary: 'Enriched summary' });
    expect(result.total).toBe(5);
    expect(result.nextCursor).toBe('cursor-1');
    expect(result.hasMore).toBe(true);
  });

  it('throws when the jobs response is not ok', async () => {
    const response = { ok: false } as Response;
    (global.fetch as jest.Mock).mockResolvedValue(response);

    await expect(fetchJobs({})).rejects.toThrow('Failed to fetch jobs');
  });

  it('fetches job detail and normalizes payload', async () => {
    const response = {
      ok: true,
      json: async () => ({
        id: 'detail-1',
        content: '<p>Detail</p>',
        enrichments: { summary: 'Detail summary', skills: ['TypeScript'] },
        enrichmentStatus: { state: 'SUCCESS' },
      }),
    } as unknown as Response;

    (global.fetch as jest.Mock).mockResolvedValue(response);

    const detail = await fetchJobDetail('detail-1');

    expect(global.fetch).toHaveBeenCalledWith('/api/jobs/detail-1/detail', { cache: 'no-store' });
    expect(detail).toMatchObject({ id: 'detail-1', summary: 'Detail summary', skills: ['TypeScript'] });
  });
});

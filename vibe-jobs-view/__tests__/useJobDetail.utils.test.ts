import { mergeJobWithDetail } from '@/modules/job-search/hooks/useJobDetail';
import type { Job, JobDetail } from '@/modules/job-search/types';

describe('mergeJobWithDetail', () => {
  const baseJob: Job = {
    id: 'job-1',
    title: 'Engineer',
    company: 'Acme',
    location: 'Remote',
    postedAt: '2024-01-01T00:00:00Z',
    url: 'https://example.com/jobs/1',
    summary: 'Base summary',
    skills: ['React'],
    highlights: ['Base highlight'],
    content: '<p>Base content</p>',
  };

  it('returns null when no job is selected', () => {
    expect(mergeJobWithDetail(null, undefined)).toBeNull();
  });

  it('returns the selected job when detail is missing', () => {
    expect(mergeJobWithDetail(baseJob, undefined)).toBe(baseJob);
  });

  it('prioritizes detail fields when provided', () => {
    const detail: JobDetail = {
      id: 'job-1',
      title: 'Engineer',
      company: 'Acme',
      location: 'Remote',
      postedAt: '2024-01-01T00:00:00Z',
      content: '<p>Detail content</p>',
      summary: 'Detail summary',
      skills: ['TypeScript'],
      highlights: [],
      structuredData: '{"key":"value"}',
    };

    const merged = mergeJobWithDetail(baseJob, detail);

    expect(merged).not.toBeNull();
    expect(merged).toMatchObject({
      content: '<p>Detail content</p>',
      summary: 'Detail summary',
      skills: ['TypeScript'],
      highlights: ['Base highlight'],
      structuredData: '{"key":"value"}',
    });
  });
});

import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchJobDetail } from '@/modules/job-search/services/jobSearchService';
import type { Job, JobDetail as JobDetailData } from '@/modules/job-search/types';

export function mergeJobWithDetail(selectedJob: Job | null, detail: JobDetailData | undefined): Job | null {
  if (!selectedJob) return null;
  if (!detail) return selectedJob;

  const baseSkills = Array.isArray(selectedJob.skills) ? selectedJob.skills : [];
  const detailSkills = Array.isArray(detail.skills) ? detail.skills : [];
  const baseHighlights = Array.isArray(selectedJob.highlights) ? selectedJob.highlights : [];
  const detailHighlights = Array.isArray(detail.highlights) ? detail.highlights : [];

  return {
    ...selectedJob,
    ...detail,
    content: detail.content ?? selectedJob.content,
    summary: detail.summary ?? selectedJob.summary,
    skills: detailSkills.length > 0 ? detailSkills : baseSkills,
    highlights: detailHighlights.length > 0 ? detailHighlights : baseHighlights,
    structuredData: detail.structuredData ?? selectedJob.structuredData,
  } satisfies Job;
}

export function useJobDetail(selectedJob: Job | null) {
  const selectedJobId = selectedJob?.id ?? null;
  const { data: detail, ...query } = useQuery<JobDetailData>({
    queryKey: ['job-detail', selectedJobId],
    queryFn: () => fetchJobDetail(selectedJobId as string),
    enabled: Boolean(selectedJobId),
  });

  const job = useMemo(() => {
    return mergeJobWithDetail(selectedJob, detail);
  }, [detail, selectedJob]);

  return {
    ...query,
    detail,
    job,
  };
}

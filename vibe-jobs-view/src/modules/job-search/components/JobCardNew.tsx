import Link from 'next/link';
import { Card, Badge } from '@/shared/ui';
import { cn } from '@/shared/lib/cn';
import { RelativeTime } from '@/shared/components/TimeDisplay';
import type { Job } from '@/modules/job-search/types';

function normalizeStringList(values?: string[] | null): string[] {
  if (!values || values.length === 0) return [];
  const seen = new Set<string>();
  const normalized: string[] = [];
  for (const value of values) {
    if (typeof value !== 'string') continue;
    const trimmed = value.trim();
    if (!trimmed || seen.has(trimmed)) continue;
    seen.add(trimmed);
    normalized.push(trimmed);
  }
  return normalized;
}

export default function JobCardNew({ job, className }: { job: Job; className?: string }) {
  const normalizedTags = normalizeStringList(job.tags).slice(0, 8);

  return (
    <Card className={cn('p-4 transition will-change-transform hover:shadow-brand-lg', className)}>
      <div className="flex gap-4">
        <div className="flex h-12 w-12 items-center justify-center rounded-2xl border border-black/5 bg-brand-50 text-brand-600">
          <svg className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
            <path d="M4 3a2 2 0 012-2h8a2 2 0 012 2v14a1 1 0 01-1.447.894L10 16.118l-4.553 1.776A1 1 0 014 16.999V3z" />
          </svg>
        </div>
        <div className="flex-1 space-y-3">
          <div className="flex items-start justify-between gap-3">
            <div className="space-y-1">
              <h3 className="text-base font-semibold leading-tight text-slate-900">
                <Link href={job.url} target="_blank" className="hover:underline decoration-brand-600 underline-offset-4">
                  {job.title}
                </Link>
              </h3>
              <p className="text-sm text-gray-600">{job.company}</p>
            </div>
            <RelativeTime utcTime={job.postedAt} className="mt-1 shrink-0 text-xs text-gray-400" />
          </div>
          <div className="flex flex-wrap items-center gap-2 text-xs text-gray-500">
            <span className="inline-flex items-center gap-1">
              <svg className="h-3.5 w-3.5" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                <path d="M10 2a6 6 0 00-6 6c0 4.418 6 10 6 10s6-5.582 6-10a6 6 0 00-6-6zm0 8a2 2 0 110-4 2 2 0 010 4z" />
              </svg>
              {job.location}
            </span>
            {job.level && (
              <span className="inline-flex items-center gap-1">
                <span className="h-1 w-1 rounded-full bg-gray-300" aria-hidden="true" />
                {job.level}
              </span>
            )}
          </div>
          {normalizedTags.length > 0 && (
            <div className="flex flex-wrap gap-2">
              {normalizedTags.map((tag) => (
                <Badge key={tag} tone="muted">
                  {tag}
                </Badge>
              ))}
            </div>
          )}
        </div>
      </div>
    </Card>
  );
}

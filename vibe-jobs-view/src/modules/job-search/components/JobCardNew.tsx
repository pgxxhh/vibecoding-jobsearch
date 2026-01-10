import Link from 'next/link';
import { Badge } from '@/shared/ui';
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
    <div className={cn('py-4 transition hover:bg-black/5', className)}>
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-3">
          <div className="space-y-1">
            <h3 className="text-base font-semibold leading-tight text-slate-900">
              <Link href={job.url} target="_blank" className="hover:underline decoration-black underline-offset-4">
                {job.title}
              </Link>
            </h3>
            <p className="text-sm text-gray-600">
              {job.company} · {job.location}
              {job.level ? ` · ${job.level}` : ''}
            </p>
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
        <RelativeTime
          utcTime={job.postedAt}
          className="mt-1 shrink-0 text-xs text-gray-500"
        />
      </div>
    </div>
  );
}

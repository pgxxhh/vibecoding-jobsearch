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
  const normalizedTags = normalizeStringList(job.tags).slice(0, 6);

  return (
    <Card className={cn('p-4 transition-all duration-200 hover:shadow-glass-lg', className)}>
      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0 flex-1 space-y-2.5">
          <div className="space-y-1.5">
            <h3 className="text-[15px] font-semibold leading-snug text-slate-900 line-clamp-2">
              <Link 
                href={job.url} 
                target="_blank" 
                className="hover:text-brand-600 transition-colors duration-200"
              >
                {job.title}
              </Link>
            </h3>
            <p className="text-sm text-slate-500 flex items-center gap-1.5 flex-wrap">
              <span className="font-medium text-slate-700">{job.company}</span>
              <span className="text-slate-300">·</span>
              <span>{job.location}</span>
              {job.level && (
                <>
                  <span className="text-slate-300">·</span>
                  <span>{job.level}</span>
                </>
              )}
            </p>
          </div>
          {normalizedTags.length > 0 && (
            <div className="flex flex-wrap gap-1.5">
              {normalizedTags.map((tag) => (
                <Badge key={tag} tone="muted" className="text-[11px]">
                  {tag}
                </Badge>
              ))}
            </div>
          )}
        </div>
        <RelativeTime
          utcTime={job.postedAt}
          className="mt-0.5 shrink-0 text-xs text-slate-400 font-medium"
        />
      </div>
    </Card>
  );
}

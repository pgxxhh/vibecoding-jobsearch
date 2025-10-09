import Link from 'next/link';
import { Card, Badge } from '@/components/ui';
import { cn } from '@/lib/cn';
import { RelativeTime } from '@/components/TimeDisplay';
import type { Job } from '@/lib/types';

export default function JobCardNew({ job, className }: { job: Job; className?: string }) {
  return (
    <Card className={cn('p-4 transition will-change-transform hover:shadow-brand-lg', className)}>
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-1">
          <h3 className="text-base font-semibold leading-tight text-slate-900">
            <Link href={job.url} target="_blank" className="hover:underline decoration-brand-600 underline-offset-4">
              {job.title}
            </Link>
          </h3>
          <p className="text-sm text-gray-600">
            {job.company} · {job.location}
            {job.level ? ` · ${job.level}` : ''}
          </p>
          <div className="mt-2 flex flex-wrap gap-2">
            {(job.tags ?? []).slice(0, 6).map((tag) => (
              <Badge key={tag} tone="muted">
                {tag}
              </Badge>
            ))}
          </div>
        </div>
        <RelativeTime 
          utcTime={job.postedAt} 
          className="mt-1 shrink-0 text-xs text-gray-500" 
        />
      </div>
    </Card>
  );
}

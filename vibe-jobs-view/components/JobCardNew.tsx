import Link from 'next/link';
import { Card, Badge } from '@/components/ui';
import { cn } from '@/lib/cn';
import { RelativeTime } from '@/components/TimeDisplay';
import type { Job } from '@/lib/types';
import { useI18n } from '@/lib/i18n';

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
  const { t } = useI18n();
  const summary = typeof job.summary === 'string' ? job.summary.trim() : '';
  const normalizedSkills = normalizeStringList(job.skills);
  const normalizedTags = normalizeStringList(job.tags);
  const displaySkills = (normalizedSkills.length > 0 ? normalizedSkills : normalizedTags).slice(0, 6);
  const highlights = normalizeStringList(job.highlights).slice(0, 3);

  return (
    <Card className={cn('p-4 transition will-change-transform hover:shadow-brand-lg', className)}>
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-3">
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
          </div>
          <p className="text-sm leading-relaxed text-gray-700">
            {summary || <span className="text-xs italic text-gray-400">{t('jobCard.summaryPlaceholder')}</span>}
          </p>
          <div className="flex flex-wrap gap-2">
            {displaySkills.length > 0 ? (
              displaySkills.map((skill) => (
                <Badge key={skill} tone="muted">
                  {skill}
                </Badge>
              ))
            ) : (
              <Badge tone="muted">{t('jobCard.skillsPlaceholder')}</Badge>
            )}
          </div>
          <div className="space-y-1 text-xs text-gray-600">
            {highlights.length > 0 ? (
              highlights.map((highlight) => (
                <div key={highlight} className="flex items-start gap-2">
                  <span className="mt-1 h-1.5 w-1.5 shrink-0 rounded-full bg-brand-500" aria-hidden />
                  <span className="flex-1 leading-relaxed">{highlight}</span>
                </div>
              ))
            ) : (
              <span className="italic text-gray-400">{t('jobCard.highlightsPlaceholder')}</span>
            )}
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

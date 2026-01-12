import { Badge, Button, Skeleton } from '@/shared/ui';
import { TimeDisplay } from '@/shared/components/TimeDisplay';
import type { Job } from '@/modules/job-search/types';

type Labels = {
  empty: string;
  summary: string;
  summaryPlaceholder: string;
  skills: string;
  skillsPlaceholder: string;
  highlights: string;
  description: string;
  noDescription: string;
  error: string;
  retry: string;
  refreshing: string;
  viewOriginal: string;
  quickApply: string;
  saveJob: string;
  enrichmentPending: string;
  enrichmentFailed: string;
};

type Props = {
  job: Job | null;
  isLoading: boolean;
  isError: boolean;
  isRefreshing: boolean;
  onRetry: () => void;
  labels: Labels;
};

const BLOCK_TAGS = ['script', 'style', 'iframe', 'object', 'embed'];
const VOID_TAGS = ['link', 'meta'];

function decodeHtmlEntities(input: string): string {
  if (!input) return '';
  return input
    .replace(/&lt;/gi, '<')
    .replace(/&gt;/gi, '>')
    .replace(/&amp;/gi, '&')
    .replace(/&quot;/gi, '"')
    .replace(/&#39;/gi, "'")
    .replace(/&nbsp;/gi, ' ')
    .replace(/&#x([0-9a-f]+);/gi, (_, hex: string) => {
      const code = parseInt(hex, 16);
      return Number.isNaN(code) ? '' : String.fromCharCode(code);
    })
    .replace(/&#(\d+);/g, (_, dec: string) => {
      const code = parseInt(dec, 10);
      return Number.isNaN(code) ? '' : String.fromCharCode(code);
    });
}

function stripDangerousTags(html: string): string {
  let output = html;
  for (const tag of BLOCK_TAGS) {
    const pattern = new RegExp(`<${tag}[^>]*>[\\s\\S]*?<\\/${tag}>`, 'gi');
    output = output.replace(pattern, '');
  }
  for (const tag of VOID_TAGS) {
    const pattern = new RegExp(`<${tag}[^>]*?>`, 'gi');
    output = output.replace(pattern, '');
  }
  return output;
}

function removeDangerousAttributes(html: string): string {
  return html
    .replace(/\son[a-z]+=("[^"]*"|'[^']*'|[^\s>]+)/gi, '')
    .replace(/(href|src)=("|')\s*javascript:[^"']*("|')/gi, '$1="#"')
    .replace(/(href|src)=\s*javascript:[^\s>]+/gi, '$1="#"');
}

function sanitizeJobContent(raw: string): string {
  if (!raw) return '';
  const decoded = decodeHtmlEntities(raw);
  const withoutTags = stripDangerousTags(decoded);
  return removeDangerousAttributes(withoutTags);
}

function normalizeStringList(values: string[] | null | undefined): string[] {
  if (!values || values.length === 0) return [];
  return values
    .map((value) => (typeof value === 'string' ? value.trim() : ''))
    .filter((value, index, array) => value.length > 0 && array.indexOf(value) === index);
}

export function resolveEnrichmentStatus(job: Job | null): Record<string, unknown> | undefined {
  if (!job) return undefined;
  if (job.enrichmentStatus && typeof job.enrichmentStatus === 'object' && !Array.isArray(job.enrichmentStatus)) {
    return job.enrichmentStatus as Record<string, unknown>;
  }
  if (job.enrichments && typeof job.enrichments === 'object' && !Array.isArray(job.enrichments)) {
    const status = (job.enrichments as Record<string, unknown>)['status'];
    if (status && typeof status === 'object' && !Array.isArray(status)) {
      return status as Record<string, unknown>;
    }
  }
  return undefined;
}

export default function JobDetail({ job, isLoading, isError, isRefreshing, onRetry, labels }: Props) {
  if (!job) {
    return (
      <div className="flex h-full min-h-[320px] flex-col items-center justify-center gap-3 text-center">
        <img src="/assets/orb-purple.svg" alt="" className="h-16 w-16 opacity-30" />
        <p className="max-w-xs text-sm text-gray-400">{labels.empty}</p>
      </div>
    );
  }

  const sanitizedContent = job.content ? sanitizeJobContent(job.content) : '';
  const hasDescription = sanitizedContent.trim().length > 0;
  const enrichmentStatus = resolveEnrichmentStatus(job);

  const normalizeStatus = (value: unknown): string | null => {
    if (typeof value !== 'string') return null;
    const trimmed = value.trim();
    return trimmed ? trimmed.toUpperCase() : null;
  };

  const statusState = normalizeStatus(enrichmentStatus?.state) ?? normalizeStatus(enrichmentStatus?.['state']);
  const shouldShowEnrichment = statusState === 'SUCCESS';
  const summary = shouldShowEnrichment && typeof job.summary === 'string' ? job.summary.trim() : '';
  const normalizedSkills = shouldShowEnrichment ? normalizeStringList(job.skills) : [];
  const normalizedTags = normalizeStringList(job.tags ?? []);
  const skillBadges = normalizedSkills.length > 0 ? normalizedSkills : normalizedTags;
  const highlights = shouldShowEnrichment ? normalizeStringList(job.highlights) : [];

  const shouldShowHighlightsSection = isLoading || (shouldShowEnrichment && highlights.length > 0);
  const handleOpenPosting = () => {
    if (!job.url || typeof window === 'undefined') return;
    window.open(job.url, '_blank', 'noopener,noreferrer');
  };

  return (
    <div className="space-y-6">
      <div className="space-y-4">
        <div className="flex flex-wrap items-center gap-2">
          {job.level && (
            <Badge tone="brand" className="uppercase tracking-wide">
              {job.level}
            </Badge>
          )}
        </div>
        <h2 className="text-2xl font-semibold text-slate-900">{job.title}</h2>
        <div className="flex flex-wrap items-center gap-3 text-sm text-gray-600">
          <span className="inline-flex items-center gap-2">
            <span className="flex h-6 w-6 items-center justify-center rounded-full bg-brand-50 text-xs font-semibold text-brand-600">
              {job.company?.charAt(0) ?? 'C'}
            </span>
            {job.company}
          </span>
          <span className="inline-flex items-center gap-2">
            <svg className="h-4 w-4 text-gray-400" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
              <path d="M10 2a6 6 0 00-6 6c0 4.418 6 10 6 10s6-5.582 6-10a6 6 0 00-6-6zm0 8a2 2 0 110-4 2 2 0 010 4z" />
            </svg>
            {job.location}
          </span>
          <span className="inline-flex items-center gap-2">
            <svg className="h-4 w-4 text-gray-400" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
              <path d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v2a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 00-1-1H6zm10 8H4a2 2 0 00-2 2v2a2 2 0 002 2h12a2 2 0 002-2v-2a2 2 0 00-2-2z" />
            </svg>
            <TimeDisplay utcTime={job.postedAt} format="DATETIME" placeholder="--" />
          </span>
          {isRefreshing && !isLoading && <span className="text-xs text-gray-400">{labels.refreshing}</span>}
        </div>
        {/*
        <div className="flex flex-wrap gap-3">
          <Button size="lg" onClick={handleOpenPosting} disabled={!job.url} className="flex-1 sm:flex-none">
            {labels.quickApply}
          </Button>
          <Button variant="outline" size="lg" type="button" className="flex-1 sm:flex-none">
            {labels.saveJob}
          </Button>
        </div>
        */}
      </div>
      {(shouldShowEnrichment || isLoading) && (
        <div className="space-y-2">
          <h3 className="text-sm font-semibold text-gray-700">{labels.summary}</h3>
          {isLoading ? (
            <div className="space-y-2">
              <Skeleton className="h-3 w-5/6" />
              <Skeleton className="h-3 w-3/4" />
            </div>
          ) : summary ? (
            <p className="text-sm leading-relaxed text-gray-800">{summary}</p>
          ) : (
            <p className="text-xs italic text-gray-400">{labels.summaryPlaceholder}</p>
          )}
        </div>
      )}
      {(shouldShowEnrichment || isLoading) && (
        <div className="space-y-2">
          <h3 className="text-sm font-semibold text-gray-700">{labels.skills}</h3>
          {isLoading ? (
            <div className="flex flex-wrap gap-2">
              <Skeleton className="h-6 w-20 rounded-full" />
              <Skeleton className="h-6 w-16 rounded-full" />
              <Skeleton className="h-6 w-24 rounded-full" />
            </div>
          ) : skillBadges.length > 0 ? (
            <div className="flex flex-wrap gap-2">
              {skillBadges.map((skill) => (
                <Badge key={skill} tone="muted">
                  {skill}
                </Badge>
              ))}
            </div>
          ) : (
            <p className="text-xs italic text-gray-400">{labels.skillsPlaceholder}</p>
          )}
        </div>
      )}
      {shouldShowHighlightsSection && (
        <div className="space-y-2">
          <h3 className="text-sm font-semibold text-gray-700">{labels.highlights}</h3>
          {isLoading ? (
            <div className="space-y-2">
              <Skeleton className="h-3 w-3/4" />
              <Skeleton className="h-3 w-2/3" />
              <Skeleton className="h-3 w-4/5" />
            </div>
          ) : (
            <ul className="space-y-1 text-sm leading-relaxed text-gray-700">
              {highlights.map((highlight) => (
                <li key={highlight} className="flex items-start gap-2">
                  <span className="mt-[6px] h-1.5 w-1.5 shrink-0 rounded-full bg-brand-500" aria-hidden />
                  <span className="flex-1">{highlight}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
      <div className="space-y-2">
        <h3 className="text-sm font-semibold text-gray-700">{labels.description}</h3>
        {isError ? (
          <div className="rounded-xl border border-red-200 bg-red-50/80 p-4 text-sm text-red-700">
            <p>{labels.error}</p>
            <Button variant="outline" size="sm" className="mt-3" onClick={onRetry}>
              {labels.retry}
            </Button>
          </div>
        ) : isLoading ? (
          <div className="space-y-2">
            <Skeleton className="h-3 w-full" />
            <Skeleton className="h-3 w-4/5" />
            <Skeleton className="h-3 w-3/5" />
          </div>
        ) : (
          <div className="mt-1 text-sm leading-relaxed text-black">
            {hasDescription ? (
              <div dangerouslySetInnerHTML={{ __html: sanitizedContent }} />
            ) : (
              labels.noDescription
            )}
          </div>
        )}
      </div>
      <div className="border-t border-slate-100 pt-4">
        <Button
          variant="outline"
          onClick={handleOpenPosting}
          className="group inline-flex justify-between border-slate-200/80 bg-white text-slate-700 hover:bg-slate-50"
          disabled={!job.url}
        >
          <span>View original job posting</span>
          <span aria-hidden className="text-base text-slate-500 transition-transform group-hover:-translate-y-[1px] group-hover:translate-x-[1px]">
            ↗
          </span>
        </Button>
      </div>
    </div>
  );
}

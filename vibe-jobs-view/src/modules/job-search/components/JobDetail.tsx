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
      <div className="flex h-full min-h-[320px] flex-col items-center justify-center gap-4 text-center">
        <div className="h-16 w-16 rounded-full bg-gradient-to-br from-slate-100 to-slate-50 flex items-center justify-center">
          <svg className="h-8 w-8 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M21 13.255A23.931 23.931 0 0112 15c-3.183 0-6.22-.62-9-1.745M16 6V4a2 2 0 00-2-2h-4a2 2 0 00-2 2v2m4 6h.01M5 20h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
          </svg>
        </div>
        <p className="max-w-xs text-sm text-slate-400">{labels.empty}</p>
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

  return (
    <div className="space-y-6">
      <div className="space-y-3">
        <h2 className="text-2xl font-bold text-slate-900 leading-tight">{job.title}</h2>
        <div className="flex items-center gap-2 text-sm text-slate-600 flex-wrap">
          <span className="font-medium text-slate-800">{job.company}</span>
          <span className="text-slate-300">·</span>
          <span>{job.location}</span>
          {job.level && (
            <>
              <span className="text-slate-300">·</span>
              <span>{job.level}</span>
            </>
          )}
        </div>
        <div className="flex items-center gap-2 text-xs text-slate-400">
          <TimeDisplay utcTime={job.postedAt} format="DATETIME" placeholder="--" />
          {isRefreshing && !isLoading && (
            <span className="flex items-center gap-1.5 text-brand-500">
              <div className="h-3 w-3 animate-spin rounded-full border border-brand-500 border-t-transparent"></div>
              {labels.refreshing}
            </span>
          )}
        </div>
      </div>

      {(shouldShowEnrichment || isLoading) && (
        <div className="space-y-3">
          <h3 className="text-sm font-semibold text-slate-700 flex items-center gap-2">
            <svg className="h-4 w-4 text-brand-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
            {labels.summary}
          </h3>
          {isLoading ? (
            <div className="space-y-2">
              <Skeleton className="h-4 w-5/6" />
              <Skeleton className="h-4 w-3/4" />
            </div>
          ) : summary ? (
            <p className="text-sm leading-relaxed text-slate-700 bg-slate-50/80 rounded-xl p-4 border border-slate-100">{summary}</p>
          ) : (
            <p className="text-xs italic text-slate-400">{labels.summaryPlaceholder}</p>
          )}
        </div>
      )}

      {(shouldShowEnrichment || isLoading) && (
        <div className="space-y-3">
          <h3 className="text-sm font-semibold text-slate-700 flex items-center gap-2">
            <svg className="h-4 w-4 text-purple-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
            </svg>
            {labels.skills}
          </h3>
          {isLoading ? (
            <div className="flex flex-wrap gap-2">
              <Skeleton className="h-7 w-20 rounded-full" />
              <Skeleton className="h-7 w-16 rounded-full" />
              <Skeleton className="h-7 w-24 rounded-full" />
            </div>
          ) : skillBadges.length > 0 ? (
            <div className="flex flex-wrap gap-2">
              {skillBadges.map((skill) => (
                <Badge key={skill} tone="brand">
                  {skill}
                </Badge>
              ))}
            </div>
          ) : (
            <p className="text-xs italic text-slate-400">{labels.skillsPlaceholder}</p>
          )}
        </div>
      )}

      {shouldShowHighlightsSection && (
        <div className="space-y-3">
          <h3 className="text-sm font-semibold text-slate-700 flex items-center gap-2">
            <svg className="h-4 w-4 text-emerald-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            {labels.highlights}
          </h3>
          {isLoading ? (
            <div className="space-y-2">
              <Skeleton className="h-4 w-3/4" />
              <Skeleton className="h-4 w-2/3" />
              <Skeleton className="h-4 w-4/5" />
            </div>
          ) : (
            <ul className="space-y-2 text-sm leading-relaxed text-slate-700">
              {highlights.map((highlight) => (
                <li key={highlight} className="flex items-start gap-3">
                  <span className="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-gradient-to-r from-brand-500 to-purple-500" aria-hidden />
                  <span className="flex-1">{highlight}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      <div className="space-y-3">
        <h3 className="text-sm font-semibold text-slate-700 flex items-center gap-2">
          <svg className="h-4 w-4 text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          {labels.description}
        </h3>
        {isError ? (
          <div className="rounded-xl border border-red-200 bg-red-50/80 p-4 text-sm text-red-700">
            <p>{labels.error}</p>
            <Button variant="outline" size="sm" className="mt-3" onClick={onRetry}>
              {labels.retry}
            </Button>
          </div>
        ) : isLoading ? (
          <div className="space-y-2">
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-4/5" />
            <Skeleton className="h-4 w-3/5" />
          </div>
        ) : (
          <div className="text-sm leading-relaxed text-slate-700 prose prose-sm prose-slate max-w-none prose-headings:text-slate-900 prose-a:text-brand-600 prose-a:no-underline hover:prose-a:underline">
            {hasDescription ? (
              <div dangerouslySetInnerHTML={{ __html: sanitizedContent }} />
            ) : (
              <p className="text-slate-400 italic">{labels.noDescription}</p>
            )}
          </div>
        )}
      </div>

      <div className="pt-2">
        <Button
          variant="outline"
          onClick={() => job.url && typeof window !== 'undefined' && window.open(job.url, '_blank', 'noopener,noreferrer')}
          disabled={!job.url}
          rightIcon={
            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
            </svg>
          }
        >
          {labels.viewOriginal}
        </Button>
      </div>
    </div>
  );
}

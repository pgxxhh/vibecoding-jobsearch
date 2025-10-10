import { Badge, Button, Skeleton } from '@/components/ui';
import { TimeDisplay } from '@/components/TimeDisplay';
import type { Job } from '@/lib/types';

type Labels = {
  empty: string;
  summary: string;
  summaryPlaceholder: string;
  skills: string;
  skillsPlaceholder: string;
  highlights: string;
  highlightsPlaceholder: string;
  description: string;
  noDescription: string;
  error: string;
  retry: string;
  refreshing: string;
  viewOriginal: string;
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
  const summary = typeof job.summary === 'string' ? job.summary.trim() : '';
  const normalizedSkills = normalizeStringList(job.skills);
  const normalizedTags = normalizeStringList(job.tags ?? []);
  const skillBadges = normalizedSkills.length > 0 ? normalizedSkills : normalizedTags;
  const highlights = normalizeStringList(job.highlights);

  return (
    <div className="space-y-6">
      <div className="space-y-2">
        <h2 className="text-2xl font-semibold text-slate-900">{job.title}</h2>
        <div className="text-sm text-gray-600">
          {job.company} · {job.location}
          {job.level ? ` · ${job.level}` : ''}
        </div>
        <div className="flex items-center gap-2 text-xs text-gray-400">
          <TimeDisplay utcTime={job.postedAt} format="DATETIME" placeholder="--" />
          {isRefreshing && !isLoading && <span>{labels.refreshing}</span>}
        </div>
      </div>
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
      <div className="space-y-2">
        <h3 className="text-sm font-semibold text-gray-700">{labels.highlights}</h3>
        {isLoading ? (
          <div className="space-y-2">
            <Skeleton className="h-3 w-3/4" />
            <Skeleton className="h-3 w-2/3" />
            <Skeleton className="h-3 w-4/5" />
          </div>
        ) : highlights.length > 0 ? (
          <ul className="space-y-1 text-sm leading-relaxed text-gray-700">
            {highlights.map((highlight) => (
              <li key={highlight} className="flex items-start gap-2">
                <span className="mt-[6px] h-1.5 w-1.5 shrink-0 rounded-full bg-brand-500" aria-hidden />
                <span className="flex-1">{highlight}</span>
              </li>
            ))}
          </ul>
        ) : (
          <p className="text-xs italic text-gray-400">{labels.highlightsPlaceholder}</p>
        )}
      </div>
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
      <Button
        variant="outline"
        onClick={() => job.url && typeof window !== 'undefined' && window.open(job.url, '_blank', 'noopener,noreferrer')}
        className="shadow-none"
        disabled={!job.url}
      >
        {labels.viewOriginal}
      </Button>
    </div>
  );
}

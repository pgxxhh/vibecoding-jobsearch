import { Badge, Button, Skeleton } from '@/components/ui';
import type { Job } from '@/lib/types';

type Labels = {
  empty: string;
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

export default function JobDetail({ job, isLoading, isError, isRefreshing, onRetry, labels }: Props) {
  if (!job) {
    return (
      <div className="flex h-full min-h-[320px] flex-col items-center justify-center gap-3 text-center">
        <img src="/assets/orb-purple.svg" alt="" className="h-16 w-16 opacity-30" />
        <p className="max-w-xs text-sm text-gray-400">{labels.empty}</p>
      </div>
    );
  }

  const posted = new Date(job.postedAt);
  const date = Number.isNaN(posted.getTime()) ? '' : posted.toLocaleDateString();
  const sanitizedContent = job.content ? sanitizeJobContent(job.content) : '';
  const hasDescription = sanitizedContent.trim().length > 0;

  return (
    <div className="space-y-6">
      <div className="space-y-2">
        <h2 className="text-2xl font-semibold text-slate-900">{job.title}</h2>
        <div className="text-sm text-gray-600">
          {job.company} · {job.location}
          {job.level ? ` · ${job.level}` : ''}
        </div>
        {date && (
          <div className="flex items-center gap-2 text-xs text-gray-400" suppressHydrationWarning>
            <span>{date}</span>
            {isRefreshing && !isLoading && <span>{labels.refreshing}</span>}
          </div>
        )}
        {!date && isRefreshing && !isLoading && (
          <div className="text-xs text-gray-400" role="status">
            {labels.refreshing}
          </div>
        )}
      </div>
      {job.tags && job.tags.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {job.tags.map((tag) => (
            <Badge key={tag} tone="muted">
              {tag}
            </Badge>
          ))}
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

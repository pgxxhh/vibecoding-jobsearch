
import type { Job } from '@/modules/job-search/types';
export default function JobCard({ job }: { job: Job }) {
  return (
    <article className="card p-5 hover:shadow-glow transition">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h3 className="text-lg font-semibold leading-tight text-brand-800">
            <a href={job.url} className="hover:underline" target="_blank" rel="noreferrer">
              {job.title}
            </a>
          </h3>
          <p className="text-sm text-brand-500 mt-1">
            {job.company} · {job.location} {job.level ? `· ${job.level}` : ''}
          </p>
          <div className="mt-2 flex flex-wrap gap-2">
            {(job.tags ?? []).slice(0, 6).map((t) => (
              <span key={t} className="badge">{t}</span>
            ))}
          </div>
        </div>
        <time className="text-xs text-brand-400 mt-1">
          {new Date(job.postedAt).toLocaleDateString()}
        </time>
      </div>
    </article>
  );
}

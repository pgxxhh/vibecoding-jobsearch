
import Card from './ui/Card'; import Badge from './ui/Badge'; import Link from 'next/link';
export type Job={id:string;title:string;company:string;location:string;level?:string;postedAt:string;tags?:string[];url:string;content?:string;};
export default function JobCardNew({job}:{job:Job}){
  const posted=new Date(job.postedAt); const date=isNaN(posted.getTime())?'':posted.toLocaleDateString();
  return <Card className="p-4 hover:shadow-brand-lg transition transform will-change-auto">
    <div className="flex items-start justify-between gap-4">
      <div className="space-y-1">
        <h3 className="text-base font-semibold leading-tight">
          <Link href={job.url} target="_blank" className="hover:underline decoration-brand-600 underline-offset-4">{job.title}</Link>
        </h3>
        <p className="text-sm text-gray-600">{job.company} · {job.location}{job.level?` · ${job.level}`:''}</p>
        <div className="mt-2 flex flex-wrap gap-2">{(job.tags??[]).slice(0,6).map(t=><Badge key={t} tone="muted">{t}</Badge>)}</div>
      </div>
      <time className="text-xs text-gray-500 mt-1 shrink-0">{date}</time>
    </div>
  </Card>;
}

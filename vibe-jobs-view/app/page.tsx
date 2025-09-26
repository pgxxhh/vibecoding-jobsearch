'use client';
import { useQuery } from '@tanstack/react-query';
import JobCardNew from '@/components/JobCardNew';
import { Badge, Button, Card, Input, Select, Skeleton } from '@/components/ui';
import { useI18n } from '@/lib/i18n';
import type { Job, JobsResponse } from '@/lib/types';
import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react';

async function fetchJobs(params: Record<string, any>): Promise<JobsResponse> {
  const qs = new URLSearchParams(Object.entries(params).filter(([,v]) => v !== '' && v !== undefined) as any);
  const res = await fetch('/api/jobs?' + qs.toString(), { cache: 'no-store' });
  if (!res.ok) throw new Error('Failed to fetch jobs');
  return res.json();
}

function SubscriptionModal({ visible, onConfirm, onCancel, params }: { visible: boolean; onConfirm: () => void; onCancel: () => void; params: Record<string, any> }) {
  const { t } = useI18n();
  if (!visible) return null;

  const formatValue = (value: string | number | undefined) => ((value ?? '') === '' ? t('forms.any') : value);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 backdrop-blur-sm px-4">
      <Card className="relative w-full max-w-lg border-white/60 bg-white/95 p-8 shadow-brand-lg">
        <button
          className="absolute right-4 top-4 text-gray-400 transition hover:text-gray-600"
          onClick={onCancel}
          aria-label={t('actions.cancel')}
        >
          ×
        </button>
        <div className="space-y-5">
          <div className="space-y-2">
            <h2 className="text-2xl font-semibold text-slate-900">{t('subscription.title')}</h2>
            <p className="text-sm text-gray-600">{t('subscription.description')}</p>
          </div>
          <div className="rounded-2xl border border-black/5 bg-gray-50/80 p-5 text-sm leading-relaxed text-gray-700">
            <div className="font-medium text-gray-900">{t('subscription.conditionsLabel')}</div>
            <div>{t('subscription.keyword', { value: formatValue(params.q) })}</div>
            <div>{t('subscription.company', { value: formatValue(params.company) })}</div>
            <div>{t('subscription.location', { value: formatValue(params.location) })}</div>
            <div>{t('subscription.level', { value: formatValue(params.level) })}</div>
          </div>
          <div className="flex justify-end gap-3">
            <Button variant="ghost" onClick={onCancel}>
              {t('actions.cancel')}
            </Button>
            <Button onClick={onConfirm}>{t('actions.confirmSubscription')}</Button>
          </div>
        </div>
      </Card>
    </div>
  );
}

function FilterDrawer({
  visible,
  onClose,
  filters,
  setFilters,
  onApply,
}: {
  visible: boolean;
  onClose: () => void;
  filters: any;
  setFilters: (f: any) => void;
  onApply: () => void;
}) {
  const { t } = useI18n();
  if (!visible) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/30 backdrop-blur-sm px-4">
      <Card className="relative w-full max-w-md border-white/60 bg-white/95 p-6 shadow-brand-lg">
        <button
          className="absolute right-4 top-4 text-gray-400 transition hover:text-gray-600"
          onClick={onClose}
          aria-label={t('filters.cancel')}
        >
          ×
        </button>
        <h2 className="mb-5 text-lg font-semibold text-slate-900">{t('filters.title')}</h2>
        <div className="space-y-5">
          <div className="space-y-2">
            <label className="text-sm font-medium text-gray-600">{t('filters.company')}</label>
            <Input
              value={filters.company}
              onChange={(event) => setFilters({ ...filters, company: event.target.value })}
              placeholder={t('filters.company')}
            />
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium text-gray-600">{t('filters.level')}</label>
            <Select
              value={filters.level}
              onChange={(event) => setFilters({ ...filters, level: event.target.value })}
            >
              <option value="">{t('forms.any')}</option>
              <option value="Junior">{t('jobLevels.junior')}</option>
              <option value="Mid">{t('jobLevels.mid')}</option>
              <option value="Senior">{t('jobLevels.senior')}</option>
              <option value="Staff">{t('jobLevels.staff')}</option>
              <option value="Principal">{t('jobLevels.principal')}</option>
            </Select>
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium text-gray-600">{t('filters.remote')}</label>
            <Select
              value={filters.remote}
              onChange={(event) => setFilters({ ...filters, remote: event.target.value })}
            >
              <option value="">{t('forms.any')}</option>
              <option value="true">{t('filters.remote.true')}</option>
              <option value="false">{t('filters.remote.false')}</option>
            </Select>
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium text-gray-600">{t('filters.salaryMin')}</label>
            <Input
              type="number"
              value={filters.salaryMin}
              onChange={(event) => setFilters({ ...filters, salaryMin: event.target.value })}
              placeholder={t('filters.salaryPlaceholder')}
            />
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium text-gray-600">{t('filters.datePosted')}</label>
            <Select
              value={filters.datePosted}
              onChange={(event) => setFilters({ ...filters, datePosted: event.target.value })}
            >
              <option value="">{t('forms.any')}</option>
              <option value="1">{t('filters.dateOptions.1')}</option>
              <option value="3">{t('filters.dateOptions.3')}</option>
              <option value="7">{t('filters.dateOptions.7')}</option>
              <option value="30">{t('filters.dateOptions.30')}</option>
            </Select>
          </div>
        </div>
        <div className="mt-6 flex justify-end gap-3">
          <Button variant="ghost" onClick={onClose}>
            {t('filters.cancel')}
          </Button>
          <Button onClick={onApply}>{t('filters.apply')}</Button>
        </div>
      </Card>
    </div>
  );
}

function JobDetail({ job }: { job: Job | null }) {
  const { t } = useI18n();
  if (!job) {
    return (
      <div className="flex h-full min-h-[320px] flex-col items-center justify-center gap-3 text-center">
        <img src="/assets/orb-purple.svg" alt="" className="h-16 w-16 opacity-30" />
        <p className="max-w-xs text-sm text-gray-400">{t('jobDetail.empty')}</p>
      </div>
    );
  }

  const posted = new Date(job.postedAt);
  const date = Number.isNaN(posted.getTime()) ? '' : posted.toLocaleDateString();

  return (
    <div className="space-y-6">
      <div className="space-y-2">
        <h2 className="text-2xl font-semibold text-slate-900">{job.title}</h2>
        <div className="text-sm text-gray-600">
          {job.company} · {job.location}
          {job.level ? ` · ${job.level}` : ''}
        </div>
        {date && <div className="text-xs text-gray-400" suppressHydrationWarning>{date}</div>}
      </div>
      {job.tags && job.tags.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {job.tags.map((tag: string) => (
            <Badge key={tag} tone="muted">
              {tag}
            </Badge>
          ))}
        </div>
      )}
      <div className="space-y-2">
        <h3 className="text-sm font-semibold text-gray-700">{t('jobDetail.description')}</h3>
        <p className="whitespace-pre-line text-sm leading-relaxed text-gray-600">
          {job.description || t('jobDetail.noDescription')}
        </p>
      </div>
      <Button
        variant="outline"
        onClick={() => window.open(job.url, '_blank', 'noopener,noreferrer')}
        className="shadow-none"
      >
        {t('jobDetail.viewOriginal')}
      </Button>
    </div>
  );
}

function HeroSection({
  q,
  setQ,
  location,
  setLocation,
  onSearch,
  onReset,
  onShowFilter,
  onShowSubscription,
  activeFilterCount,
  isSearching,
}: {
  q: string;
  setQ: (v: string) => void;
  location: string;
  setLocation: (v: string) => void;
  onSearch: (event: FormEvent<HTMLFormElement>) => void;
  onReset: () => void;
  onShowFilter: () => void;
  onShowSubscription: () => void;
  activeFilterCount: number;
  isSearching: boolean;
}) {
  const { t } = useI18n();

  return (
    <section className="relative overflow-hidden rounded-[2.75rem] border border-white/50 bg-white/80 p-8 shadow-brand-lg backdrop-blur">
      <img src="/assets/orb-rose.svg" alt="" className="pointer-events-none absolute -left-20 top-[-40%] h-96 w-96 opacity-40" />
      <img src="/assets/orb-purple.svg" alt="" className="pointer-events-none absolute -right-16 bottom-[-30%] h-96 w-96 opacity-30" />
      <div className="relative grid gap-10 lg:grid-cols-[minmax(0,1fr)_360px]">
        <div className="space-y-5">
          <Badge tone="brand" className="w-fit">
            {t('hero.badge')}
          </Badge>
          <div className="space-y-4">
            <h1 className="max-w-xl text-3xl font-semibold tracking-tight text-slate-900 sm:text-4xl">
              {t('hero.title')}
            </h1>
            <p className="max-w-xl text-base text-gray-600 sm:text-lg">{t('hero.subtitle')}</p>
          </div>
        </div>
        <Card className="border-white/60 bg-white/95 p-6 shadow-brand-lg backdrop-blur-sm">
          <form className="flex flex-col gap-4" onSubmit={onSearch}>
            <div className="grid gap-3">
              <Input
                placeholder={t('search.keywordPlaceholder')}
                value={q}
                onChange={(event) => setQ(event.target.value)}
              />
              <Input
                placeholder={t('search.locationPlaceholder')}
                value={location}
                onChange={(event) => setLocation(event.target.value)}
              />
            </div>
            <div className="flex flex-wrap items-center gap-3">
              <Button type="submit" disabled={isSearching}>
                {isSearching ? t('search.loading') : t('actions.search')}
              </Button>
              <Button variant="outline" type="button" onClick={onReset}>
                {t('actions.reset')}
              </Button>
              <Button variant="ghost" type="button" onClick={onShowFilter} className="flex items-center gap-2">
                {t('filters.open')}
                {activeFilterCount > 0 && (
                  <span className="flex h-6 w-6 items-center justify-center rounded-full bg-brand-100 text-xs font-medium text-brand-700">
                    {activeFilterCount}
                  </span>
                )}
              </Button>
              <Button variant="ghost" type="button" onClick={onShowSubscription}>
                {t('actions.createSubscription')}
              </Button>
            </div>
          </form>
        </Card>
      </div>
    </section>
  );
}

export default function Page() {
  const { t } = useI18n();
  const [q, setQ] = useState('');
  const [location, setLocation] = useState('');
  const [page, setPage] = useState(1);
  const size = 10;
  // 筛选弹窗相关状态
  const [showFilterDrawer, setShowFilterDrawer] = useState(false);
  const [filters, setFilters] = useState({ company: '', level: '', remote: '', salaryMin: '', datePosted: '' });
  const [showSubscriptionModal, setShowSubscriptionModal] = useState(false);
  const hasPromptedSubscription = useRef(false);
  const [subscriptionTrigger, setSubscriptionTrigger] = useState<'search' | 'manual' | null>(null);
  const [selectedJob, setSelectedJob] = useState<Job | null>(null);

  const params = useMemo(() => ({ q, location, page, size, ...filters }), [q, location, page, size, filters]);
  const { data, isLoading, isError, refetch, isFetching } = useQuery({
    queryKey: ['jobs', params],
    queryFn: () => fetchJobs(params),
    keepPreviousData: true,
  });

  const jobs = data?.items ?? [];
  const total = data?.total ?? 0;
  const currentPage = data?.page ?? page;
  const pageSize = data?.size ?? size;
  const isSearching = isLoading || isFetching;

  useEffect(() => {
    setSelectedJob((current) => {
      if (!jobs.length) return null;
      if (current && jobs.some((item) => item.id === current.id)) {
        return current;
      }
      return jobs[0];
    });
  }, [jobs]);

  const activeFilterCount = useMemo(
    () => Object.values(filters).filter((value) => value !== '' && value !== undefined).length,
    [filters],
  );

  const handleReset = () => {
    setQ('');
    setLocation('');
    setFilters({ company: '', level: '', remote: '', salaryMin: '', datePosted: '' });
    setPage(1);
    setSelectedJob(null);
  };

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setPage(1);
    if (!hasPromptedSubscription.current) {
      setShowSubscriptionModal(true);
      setSubscriptionTrigger('search');
      hasPromptedSubscription.current = true;
    } else {
      refetch();
    }
  };

  const handleManualSubscription = () => {
    setShowSubscriptionModal(true);
    setSubscriptionTrigger('manual');
  };

  const handleConfirmSubscription = async () => {
    setShowSubscriptionModal(false);
    setSubscriptionTrigger(null);
    // 调用订阅接口
    await fetch('/api/subscription', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(params),
    });
    refetch();
  };

  const handleCancelSubscription = () => {
    setShowSubscriptionModal(false);
    setSubscriptionTrigger(null);
    if (subscriptionTrigger === 'search') refetch();
  };

  const handleApplyFilters = () => {
    setShowFilterDrawer(false);
    setPage(1);
    refetch();
  };

  const skeletonPlaceholders = Array.from({ length: 4 });

  return (
    <div className="space-y-10 pb-16">
      <HeroSection
        q={q}
        setQ={setQ}
        location={location}
        setLocation={setLocation}
        onSearch={handleSearch}
        onReset={handleReset}
        onShowFilter={() => setShowFilterDrawer(true)}
        onShowSubscription={handleManualSubscription}
        activeFilterCount={activeFilterCount}
        isSearching={isSearching}
      />

      <div className="grid gap-6 lg:grid-cols-[minmax(0,420px)_minmax(0,1fr)]">
        <Card className="border-white/60 bg-white/90 p-6 shadow-brand-lg backdrop-blur-sm lg:max-h-[70vh] lg:overflow-hidden">
          <div className="flex items-center justify-between gap-3">
            <p className="text-sm text-gray-500">
              {isLoading ? t('search.loading') : t('search.results', { count: total })}
              {isFetching && !isLoading ? ` · ${t('search.refreshing')}` : ''}
            </p>
            <span className="text-xs text-gray-400">{t('search.page', { page: currentPage })}</span>
          </div>
          <div className="mt-4 space-y-3 overflow-y-auto pr-1 lg:max-h-[52vh]">
            {isError && (
              <Card className="border-dashed border-red-200 bg-red-50/70 p-4 text-sm text-red-600">
                {t('errors.fetchJobs')}
              </Card>
            )}
            {isLoading &&
              skeletonPlaceholders.map((_, index) => (
                <Card key={`skeleton-${index}`} className="border-dashed border-black/5 bg-white/70 p-4">
                  <Skeleton className="h-4 w-3/4" />
                  <Skeleton className="mt-3 h-3 w-1/2" />
                  <div className="mt-4 flex gap-2">
                    <Skeleton className="h-3 w-16" />
                    <Skeleton className="h-3 w-20" />
                    <Skeleton className="h-3 w-12" />
                  </div>
                </Card>
              ))}
            {!isLoading && !isError &&
              jobs.map((job) => {
                const active = selectedJob?.id === job.id;
                return (
                  <div key={job.id} className="cursor-pointer" onClick={() => setSelectedJob(job)}>
                    <JobCardNew
                      job={job}
                      className={active ? 'border-brand-500 shadow-brand-lg ring-2 ring-brand-200' : 'hover:border-brand-200/70'}
                    />
                  </div>
                );
              })}
            {!isLoading && !isError && jobs.length === 0 && (
              <div className="flex h-40 items-center justify-center rounded-3xl border border-dashed border-black/10 bg-white/70 text-sm text-gray-400">
                {t('search.results', { count: 0 })}
              </div>
            )}
          </div>
          <div className="mt-6 flex items-center justify-between gap-3">
            <Button
              variant="outline"
              onClick={() => setPage((previous) => Math.max(1, previous - 1))}
              disabled={currentPage <= 1 || isLoading || isFetching}
            >
              {t('actions.previous')}
            </Button>
            <Button
              variant="outline"
              onClick={() => setPage((previous) => previous + 1)}
              disabled={currentPage * pageSize >= total || isLoading || isFetching}
            >
              {t('actions.next')}
            </Button>
          </div>
        </Card>

        <Card className="border-white/60 bg-white/95 p-6 shadow-brand-lg backdrop-blur-sm lg:max-h-[70vh] lg:overflow-y-auto">
          <JobDetail job={selectedJob} />
        </Card>
      </div>

      <SubscriptionModal
        visible={showSubscriptionModal}
        onConfirm={handleConfirmSubscription}
        onCancel={handleCancelSubscription}
        params={params}
      />

      <FilterDrawer
        visible={showFilterDrawer}
        onClose={() => setShowFilterDrawer(false)}
        filters={filters}
        setFilters={setFilters}
        onApply={handleApplyFilters}
      />
    </div>
  );
}

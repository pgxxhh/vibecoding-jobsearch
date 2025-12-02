'use client';
import JobDetail from '@/modules/job-search/components/JobDetail';
import JobCardNew from '@/modules/job-search/components/JobCardNew';
import { useJobDetail } from '@/modules/job-search/hooks/useJobDetail';
import { useJobList, type JobListFilters } from '@/modules/job-search/hooks/useJobList';
import type { Job } from '@/modules/job-search/types';
import { useI18n } from '@/shared/lib/i18n';
import { Badge, Button, Card, Input, Select, Skeleton } from '@/shared/ui';
import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type Dispatch,
  type FormEvent,
  type SetStateAction,
} from 'react';

function SubscriptionModal({ visible, onConfirm, onCancel, params }: { visible: boolean; onConfirm: () => void; onCancel: () => void; params: Record<string, any> }) {
  const { t } = useI18n();
  if (!visible) return null;

  const formatValue = (value: string | number | undefined): string | number => {
    if (value === undefined || value === '') {
      return t('forms.any');
    }
    return value;
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-md px-4">
      <Card className="relative w-full max-w-lg border-slate-100 bg-white/98 p-8 shadow-glass-lg animate-scale-in">
        <button
          className="absolute right-5 top-5 flex h-8 w-8 items-center justify-center rounded-full text-slate-400 transition-all hover:bg-slate-100 hover:text-slate-600"
          onClick={onCancel}
          aria-label={t('actions.cancel')}
        >
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
        <div className="space-y-6">
          <div className="space-y-2">
            <h2 className="text-2xl font-bold text-slate-900">{t('subscription.title')}</h2>
            <p className="text-sm text-slate-500">{t('subscription.description')}</p>
          </div>
          <div className="rounded-2xl border border-slate-100 bg-slate-50/80 p-5 text-sm leading-relaxed text-slate-600">
            <div className="font-semibold text-slate-800 mb-2">{t('subscription.conditionsLabel')}</div>
            <div className="space-y-1">
              <div>{t('subscription.keyword', { value: formatValue(params.q ?? '') })}</div>
              <div>{t('subscription.company', { value: formatValue(params.company ?? '') })}</div>
              <div>{t('subscription.location', { value: formatValue(params.location ?? '') })}</div>
              <div>{t('subscription.level', { value: formatValue(params.level ?? '') })}</div>
            </div>
          </div>
          <div className="flex justify-end gap-2.5">
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
  filters: JobListFilters;
  setFilters: Dispatch<SetStateAction<JobListFilters>>;
  onApply: () => void;
}) {
  const { t } = useI18n();
  if (!visible) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-md px-4">
      <Card className="relative w-full max-w-md border-slate-100 bg-white/98 p-6 shadow-glass-lg animate-scale-in">
        <button
          className="absolute right-5 top-5 flex h-8 w-8 items-center justify-center rounded-full text-slate-400 transition-all hover:bg-slate-100 hover:text-slate-600"
          onClick={onClose}
          aria-label={t('filters.cancel')}
        >
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
        <h2 className="mb-6 text-xl font-bold text-slate-900">{t('filters.title')}</h2>
        <div className="space-y-5">
          <div className="space-y-2">
            <label className="text-sm font-medium text-slate-700">{t('filters.company')}</label>
            <Input
              value={filters.company}
              onChange={(event) => setFilters({ ...filters, company: event.target.value })}
              placeholder={t('filters.company')}
            />
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium text-slate-700">{t('filters.datePosted')}</label>
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
        <div className="mt-8 flex justify-end gap-2.5">
          <Button variant="ghost" onClick={onClose}>
            {t('filters.cancel')}
          </Button>
          <Button onClick={onApply}>{t('filters.apply')}</Button>
        </div>
      </Card>
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
  activeFilterCount: number;
  isSearching: boolean;
}) {
  const { t } = useI18n();

  return (
    <section className="relative overflow-hidden rounded-[2rem] border border-white/60 bg-white/70 p-8 shadow-glass-lg backdrop-blur-xl lg:p-10">
      <div className="pointer-events-none absolute -left-32 -top-32 h-80 w-80 rounded-full bg-gradient-to-br from-brand-300/30 to-purple-300/20 blur-3xl" />
      <div className="pointer-events-none absolute -right-24 -bottom-24 h-72 w-72 rounded-full bg-gradient-to-tl from-brand-200/25 to-pink-200/15 blur-3xl" />
      <div className="relative grid gap-8 lg:grid-cols-[minmax(0,1fr)_380px] lg:gap-12">
        <div className="space-y-6">
          <Badge tone="brand" className="w-fit animate-fade-in">
            <span className="mr-1 inline-block h-1.5 w-1.5 rounded-full bg-brand-500 animate-pulse" />
            {t('hero.badge')}
          </Badge>
          <div className="space-y-4">
            <h1 className="max-w-xl text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl lg:text-[2.75rem] lg:leading-tight">
              {t('hero.title')}
            </h1>
            <p className="max-w-xl text-base text-slate-600 sm:text-lg">{t('hero.subtitle')}</p>
          </div>
        </div>
        <Card className="border-slate-100/80 bg-white/90 p-6 shadow-glass-lg backdrop-blur-sm">
          <form className="flex flex-col gap-4" onSubmit={onSearch}>
            <div className="grid gap-3.5">
              <div className="relative">
                <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400">
                  <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                </span>
                <Input
                  placeholder={t('search.keywordPlaceholder')}
                  value={q}
                  onChange={(event) => setQ(event.target.value)}
                  className="pl-10"
                />
              </div>
              <div className="relative">
                <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400">
                  <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                </span>
                <Input
                  placeholder={t('search.locationPlaceholder')}
                  value={location}
                  onChange={(event) => setLocation(event.target.value)}
                  className="pl-10"
                />
              </div>
            </div>
            <div className="flex flex-wrap items-center gap-2.5">
              <Button type="submit" disabled={isSearching} className="flex-1 sm:flex-none">
                {isSearching ? t('search.loading') : t('actions.search')}
              </Button>
              <Button variant="outline" type="button" onClick={onReset}>
                {t('actions.reset')}
              </Button>
              <Button variant="ghost" type="button" onClick={onShowFilter} className="flex items-center gap-2">
                <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z" />
                </svg>
                {t('filters.open')}
                {activeFilterCount > 0 && (
                  <span className="flex h-5 w-5 items-center justify-center rounded-full bg-brand-500 text-[10px] font-semibold text-white">
                    {activeFilterCount}
                  </span>
                )}
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
  const [filters, setFilters] = useState<JobListFilters>({
    company: '',
    level: '',
    remote: '',
    salaryMin: '',
    datePosted: '',
  });
  const [showFilterDrawer, setShowFilterDrawer] = useState(false);
  // const [showSubscriptionModal, setShowSubscriptionModal] = useState(false);
  // const hasPromptedSubscription = useRef(false);
  // const [subscriptionTrigger, setSubscriptionTrigger] = useState<'search' | null>(null);
  const [selectedJob, setSelectedJob] = useState<Job | null>(null);
  const [isMobile, setIsMobile] = useState(false);
  const [isMobileDetailOpen, setIsMobileDetailOpen] = useState(false);

  useEffect(() => {
    if (typeof window === 'undefined') return;
    const handleResize = () => {
      setIsMobile(window.innerWidth < 1024);
    };

    handleResize();
    window.addEventListener('resize', handleResize);
    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, []);

  useEffect(() => {
    if (!isMobile) {
      setIsMobileDetailOpen(false);
    }
  }, [isMobile]);

  const jobDetailLabels = useMemo(
    () => ({
      empty: t('jobDetail.empty'),
      summary: t('jobDetail.summary'),
      summaryPlaceholder: t('jobDetail.summaryPlaceholder'),
      skills: t('jobDetail.skills'),
      skillsPlaceholder: t('jobDetail.skillsPlaceholder'),
      highlights: t('jobDetail.highlights'),
      description: t('jobDetail.description'),
      noDescription: t('jobDetail.noDescription'),
      error: t('jobDetail.error'),
      retry: t('actions.retry'),
      refreshing: t('jobDetail.refreshing'),
      viewOriginal: t('jobDetail.viewOriginal'),
      enrichmentPending: t('jobDetail.enrichmentPending'),
      enrichmentFailed: t('jobDetail.enrichmentFailed'),
    }),
    [t],
  );

  // 列表区域ref和底部检测ref
  const listRef = useRef<HTMLDivElement>(null);
  const loadMoreRef = useRef<HTMLDivElement>(null);

  const handleJobListReset = useCallback(
    (items: Job[]) => {
      if (items.length === 0) {
        setSelectedJob(null);
        if (isMobile) {
          setIsMobileDetailOpen(false);
        }
        return;
      }

      setSelectedJob((previous) => {
        if (isMobile && previous) {
          const stillExists = items.some((item) => item.id === previous.id);
          if (stillExists) {
            return previous;
          }
        }
        return items[0];
      });

      if (isMobile) {
        setIsMobileDetailOpen(false);
      }
    },
    [isMobile],
  );

  const {
    jobs,
    hasMore,
    isLoading: isListLoading,
    isInitialLoading,
    refresh,
    loadMore,
    nextCursor,
  } = useJobList({
    query: q,
    location,
    filters,
    onReset: handleJobListReset,
  });

  // 使用 Intersection Observer 检测底部元素
  useEffect(() => {
    const loadMoreElement = loadMoreRef.current;
    if (!loadMoreElement) return;

    const listElement = listRef.current;
    const isScrollable = !!listElement && listElement.scrollHeight > listElement.clientHeight + 1;

    const observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0];
        if (entry.isIntersecting && hasMore && !isListLoading && nextCursor) {
          loadMore();
        }
      },
      {
        root: isScrollable ? listElement : null,
        rootMargin: '20px', // 提前20px触发
        threshold: 0.1,
      }
    );

    observer.observe(loadMoreElement);

    return () => {
      observer.disconnect();
    };
  }, [hasMore, isListLoading, nextCursor, jobs.length, isMobile, loadMore]);


  // 过滤计数
  const activeFilterCount = useMemo(
    () => Object.values(filters).filter((value) => value !== '' && value !== undefined).length,
    [filters],
  );

  // const subscriptionParams = useMemo(
  //   () => ({
  //     q,
  //     location,
  //     ...filters,
  //   }),
  //   [q, location, filters],
  // );

  const {
    job: combinedSelectedJob,
    isLoading: isDetailLoading,
    isError: isDetailError,
    isFetching: isDetailFetching,
    refetch: refetchJobDetail,
  } = useJobDetail(selectedJob);

  // 滚动加载 - 支持桌面和移动端
  const handleScroll = () => {
    const el = listRef.current;
    if (!el || isListLoading || !hasMore || !nextCursor) return;
    const threshold = 100; // 增加阈值，提高移动端触发灵敏度
    const scrollPosition = el.scrollTop + el.clientHeight;
    const scrollHeight = el.scrollHeight;

    if (scrollHeight - scrollPosition < threshold) {
      // 只追加，不重置
      loadMore();
    }
  };

  // 移动端下拉刷新状态
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [pullDistance, setPullDistance] = useState(0);
  const [touchStart, setTouchStart] = useState({ y: 0, time: 0 });

  // 下拉刷新处理
  const handleTouchStart = (e: React.TouchEvent) => {
    const touch = e.touches[0];
    setTouchStart({ y: touch.clientY, time: Date.now() });
  };

  const handleTouchMove = (e: React.TouchEvent) => {
    const el = listRef.current;
    if (!el || el.scrollTop > 0) return; // 只在顶部才启用下拉刷新

    const touch = e.touches[0];
    const distance = touch.clientY - touchStart.y;
    
    if (distance > 0 && distance < 100) { // 限制下拉距离
      setPullDistance(distance);
      e.preventDefault(); // 防止默认的页面滚动
    }
  };

  const handleTouchEndForRefresh = async () => {
    if (pullDistance > 50 && !isRefreshing) { // 下拉超过50px触发刷新
      setIsRefreshing(true);
      try {
        await refresh(); // 重新加载列表
      } finally {
        setIsRefreshing(false);
      }
    }
    setPullDistance(0);
  };

  // 移动端触摸事件处理（原有的滚动检测）
  const handleTouchEnd = () => {
    handleTouchEndForRefresh();
    // 延迟检查，确保滚动完成
    setTimeout(handleScroll, 100);
  };

  // 添加防抖处理
  const [scrollTimer, setScrollTimer] = useState<NodeJS.Timeout | null>(null);
  
  const debouncedHandleScroll = () => {
    if (scrollTimer) clearTimeout(scrollTimer);
    const timer = setTimeout(handleScroll, 150);
    setScrollTimer(timer);
  };

  // 清理定时器
  useEffect(() => {
    return () => {
      if (scrollTimer) clearTimeout(scrollTimer);
    };
  }, [scrollTimer]);

  const skeletonPlaceholders = Array.from({ length: 4 });

  // 搜索/重置/筛选
  const handleReset = () => {
    setQ('');
    setLocation('');
    setFilters({ company: '', level: '', remote: '', salaryMin: '', datePosted: '' });
    refresh();
  };

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    // if (!hasPromptedSubscription.current) {
    //   setShowSubscriptionModal(true);
    //   setSubscriptionTrigger('search');
    //   hasPromptedSubscription.current = true;
    // } else {
    //   refresh();
    // }
    refresh();
  };

  // const handleConfirmSubscription = async () => {
  //   setShowSubscriptionModal(false);
  //   setSubscriptionTrigger(null);
  //   await fetch(`${API_BASE}/subscription`, {
  //     method: 'POST',
  //     headers: { 'Content-Type': 'application/json' },
  //     body: JSON.stringify(subscriptionParams),
  //   });
  //   refresh();
  // };

  // const handleCancelSubscription = () => {
  //   setShowSubscriptionModal(false);
  //   setSubscriptionTrigger(null);
  //   if (subscriptionTrigger === 'search') refresh();
  // };

  const handleApplyFilters = () => {
    setShowFilterDrawer(false);
    refresh();
  };

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
        activeFilterCount={activeFilterCount}
        isSearching={isListLoading}
      />

      <div className="grid gap-6 lg:grid-cols-[minmax(0,420px)_minmax(0,1fr)]">
        <Card className="border-slate-100/80 bg-white/90 p-5 shadow-glass-lg backdrop-blur-sm lg:max-h-[70vh] lg:overflow-hidden relative">
          {/* 下拉刷新指示器 */}
          {(pullDistance > 0 || isRefreshing) && (
            <div 
              className="absolute top-0 left-0 right-0 flex items-center justify-center bg-brand-50/80 border-b border-brand-100/50 transition-all duration-200 backdrop-blur-sm rounded-t-3xl"
              style={{ 
                height: `${Math.max(pullDistance, isRefreshing ? 60 : 0)}px`,
                transform: `translateY(-${Math.max(pullDistance, isRefreshing ? 60 : 0)}px)`
              }}
            >
              <div className="flex items-center gap-2 text-brand-600">
                {isRefreshing ? (
                  <>
                    <div className="animate-spin rounded-full h-4 w-4 border-2 border-brand-500 border-t-transparent"></div>
                    <span className="text-sm font-medium">正在刷新...</span>
                  </>
                ) : pullDistance > 50 ? (
                  <span className="text-sm font-medium">松开刷新</span>
                ) : (
                  <span className="text-sm font-medium">下拉刷新</span>
                )}
              </div>
            </div>
          )}
          
          <div
            className="space-y-3 overflow-y-auto pr-1 lg:max-h-[60vh]"
            ref={listRef}
            onScroll={debouncedHandleScroll}
            onTouchStart={handleTouchStart}
            onTouchMove={handleTouchMove}
            onTouchEnd={handleTouchEnd}
            style={{ 
              WebkitOverflowScrolling: 'touch',
              overscrollBehavior: 'contain',
              transform: `translateY(${pullDistance}px)`,
              transition: pullDistance === 0 ? 'transform 0.2s ease-out' : 'none'
            }}
          >
            {isInitialLoading &&
              skeletonPlaceholders.map((_, index) => (
                <Card key={`skeleton-${index}`} className="border-dashed border-slate-100 bg-white/80 p-4 animate-pulse">
                  <Skeleton className="h-5 w-3/4" />
                  <Skeleton className="mt-3 h-4 w-1/2" />
                  <div className="mt-4 flex gap-2">
                    <Skeleton className="h-6 w-16 rounded-full" />
                    <Skeleton className="h-6 w-20 rounded-full" />
                    <Skeleton className="h-6 w-14 rounded-full" />
                  </div>
                </Card>
              ))}
            {jobs.map((job) => {
              const active = selectedJob?.id === job.id;
              return (
                <div
                  key={job.id}
                  className="cursor-pointer transition-transform duration-200 hover:scale-[1.01]"
                  onClick={() => {
                    setSelectedJob(job);
                    if (isMobile) {
                      setIsMobileDetailOpen(true);
                    }
                  }}
                >
                  <JobCardNew
                    job={job}
                    className={active ? 'border-brand-400 shadow-brand-md ring-2 ring-brand-100' : 'hover:border-slate-200 hover:shadow-md'}
                  />
                </div>
              );
            })}
            {!isInitialLoading && jobs.length === 0 && (
              <div className="flex h-48 flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-slate-50/50 text-center">
                <svg className="h-10 w-10 text-slate-300 mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                <p className="text-sm text-slate-400">{t('search.results', { count: 0 })}</p>
              </div>
            )}
            {/* 加载更多指示器和观察器元素 */}
            {isListLoading && jobs.length > 0 && (
              <div className="flex justify-center py-4">
                <div className="flex items-center gap-2 text-sm text-slate-400">
                  <div className="h-4 w-4 animate-spin rounded-full border-2 border-slate-300 border-t-brand-500"></div>
                  <span>加载中...</span>
                </div>
              </div>
            )}
            {!hasMore && jobs.length > 0 && (
              <div className="text-center text-sm text-slate-400 py-4">没有更多数据了</div>
            )}
            {/* Intersection Observer 目标元素 */}
            <div ref={loadMoreRef} className="h-1 w-full" />
          </div>
        </Card>

        <Card className="hidden border-slate-100/80 bg-white/95 p-6 shadow-glass-lg backdrop-blur-sm lg:block lg:max-h-[70vh] lg:overflow-y-auto">
          <JobDetail
            job={combinedSelectedJob}
            isLoading={isDetailLoading}
            isError={isDetailError}
            isRefreshing={isDetailFetching}
            onRetry={() => refetchJobDetail()}
            labels={jobDetailLabels}
          />
        </Card>
      </div>

      {isMobile && isMobileDetailOpen && selectedJob && (
        <div className="fixed inset-0 z-50 flex flex-col bg-white/98 backdrop-blur-xl">
          <div className="flex items-center justify-between border-b border-slate-100 px-4 py-3.5 bg-white/90">
            <div className="min-w-0 flex-1 pr-3">
              <p className="truncate text-sm font-semibold text-slate-800">{selectedJob.title}</p>
              <p className="truncate text-xs text-slate-500">
                {selectedJob.company} · {selectedJob.location}
              </p>
            </div>
            <Button variant="ghost" size="sm" onClick={() => setIsMobileDetailOpen(false)}>
              {t('actions.cancel')}
            </Button>
          </div>
          <div className="flex-1 overflow-y-auto px-5 py-6">
            <JobDetail
              job={combinedSelectedJob}
              isLoading={isDetailLoading}
              isError={isDetailError}
              isRefreshing={isDetailFetching}
              onRetry={() => refetchJobDetail()}
              labels={jobDetailLabels}
            />
          </div>
        </div>
      )}

      {/*
      <SubscriptionModal
        visible={showSubscriptionModal}
        onConfirm={handleConfirmSubscription}
        onCancel={handleCancelSubscription}
        params={subscriptionParams}
      />
      */}

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

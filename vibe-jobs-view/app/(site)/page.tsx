'use client';
import { useQuery } from '@tanstack/react-query';
import JobDetail from '@/components/JobDetail';
import JobCardNew from '@/components/JobCardNew';
import { Badge, Button, Card, Input, Select, Skeleton } from '@/components/ui';
import { useI18n } from '@/lib/i18n';
import type { Job, JobDetail as JobDetailData, JobsResponse } from '@/lib/types';
import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react';

const API_BASE =
  process.env.NEXT_PUBLIC_BACKEND_BASE ??
  process.env.NEXT_PUBLIC_API_BASE ??
  '/api';

async function fetchJobs(params: Record<string, any>): Promise<JobsResponse> {
  const qs = new URLSearchParams(
    Object.entries(params).filter(([, v]) => v !== '' && v !== undefined && v !== null) as any,
  );
  const res = await fetch(`${API_BASE}/jobs?` + qs.toString(), { cache: 'no-store' });
  if (!res.ok) throw new Error('Failed to fetch jobs');
  return res.json();
}

async function fetchJobDetail(id: string): Promise<JobDetailData> {
  const res = await fetch(`${API_BASE}/jobs/${id}/detail`, { cache: 'no-store' });
  if (!res.ok) throw new Error('Failed to fetch job detail');
  const detail = await res.json();
  return {
    id: String(detail.id ?? id),
    title: detail.title ?? '',
    company: detail.company ?? '',
    location: detail.location ?? '',
    postedAt: detail.postedAt ?? '',
    content: detail.content ?? '',
  };
}

const PAGE_SIZE = 10;
const MAX_FILTER_PAGINATION_FETCHES = 5;

function computeDateCutoff(daysValue: string): number | null {
  const days = Number(daysValue);
  if (!Number.isFinite(days) || days <= 0) return null;
  return Date.now() - days * 24 * 60 * 60 * 1000;
}

function filterJobsByDate(items: Job[], cutoff: number | null): Job[] {
  if (!cutoff) return items;
  return items.filter(item => {
    const postedAtMillis = new Date(item.postedAt).getTime();
    return Number.isFinite(postedAtMillis) && postedAtMillis >= cutoff;
  });
}

function encodeCursorValue(postedAt: string, id: string | number): string | null {
  const postedAtMillis = new Date(postedAt).getTime();
  const numericId = Number(id);
  if (!Number.isFinite(postedAtMillis) || !Number.isFinite(numericId)) {
    return null;
  }
  const payload = `${postedAtMillis}:${numericId}`;
  if (typeof window === 'undefined' || typeof window.btoa !== 'function') {
    return null;
  }
  try {
    const base64 = window.btoa(payload);
    return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  } catch {
    return null;
  }
}

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
            <div>{t('subscription.keyword', { value: formatValue(params.q ?? '') })}</div>
            <div>{t('subscription.company', { value: formatValue(params.company ?? '') })}</div>
            <div>{t('subscription.location', { value: formatValue(params.location ?? '') })}</div>
            <div>{t('subscription.level', { value: formatValue(params.level ?? '') })}</div>
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
  const [filters, setFilters] = useState({ company: '', level: '', remote: '', salaryMin: '', datePosted: '' });
  const [showFilterDrawer, setShowFilterDrawer] = useState(false);
  const [showSubscriptionModal, setShowSubscriptionModal] = useState(false);
  const hasPromptedSubscription = useRef(false);
  const [subscriptionTrigger, setSubscriptionTrigger] = useState<'search' | 'manual' | null>(null);
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
      description: t('jobDetail.description'),
      noDescription: t('jobDetail.noDescription'),
      error: t('jobDetail.error'),
      retry: t('actions.retry'),
      refreshing: t('jobDetail.refreshing'),
      viewOriginal: t('jobDetail.viewOriginal'),
    }),
    [t],
  );

  // 列表数据和分页状态
  const [jobs, setJobs] = useState<Job[]>([]);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);

  // 列表区域ref和底部检测ref
  const listRef = useRef<HTMLDivElement>(null);
  const loadMoreRef = useRef<HTMLDivElement>(null);

  // 使用 Intersection Observer 检测底部元素
  useEffect(() => {
    const loadMoreElement = loadMoreRef.current;
    if (!loadMoreElement) return;

    const listElement = listRef.current;
    const isScrollable = !!listElement && listElement.scrollHeight > listElement.clientHeight + 1;

    const observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0];
        if (entry.isIntersecting && hasMore && !loading && nextCursor) {
          loadJobs(nextCursor, false);
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
  }, [hasMore, loading, nextCursor, jobs.length, isMobile]);

  // 过滤计数
  const activeFilterCount = useMemo(
    () => Object.values(filters).filter((value) => value !== '' && value !== undefined).length,
    [filters],
  );

  const subscriptionParams = useMemo(
    () => ({
      q,
      location,
      ...filters,
    }),
    [q, location, filters],
  );

  const selectedJobId = selectedJob?.id;

  const {
    data: jobDetail,
    isLoading: isDetailLoading,
    isError: isDetailError,
    isFetching: isDetailFetching,
    refetch: refetchJobDetail,
  } = useQuery<JobDetailData>({
    queryKey: ['job-detail', selectedJobId],
    queryFn: () => fetchJobDetail(selectedJobId as string),
    enabled: !!selectedJobId,
  });

  const combinedSelectedJob = useMemo<Job | null>(() => {
    if (!selectedJob) return null;
    if (!jobDetail) return selectedJob;
    return {
      ...selectedJob,
      ...jobDetail,
      content: jobDetail.content ?? selectedJob.content,
    };
  }, [selectedJob, jobDetail]);

  // 加载数据
  const loadJobs = async (cursor?: string, reset = false) => {
    setLoading(true);
    try {
      const baseParams: Record<string, any> = { q, location, size: PAGE_SIZE, ...filters };
      const cutoff = computeDateCutoff(filters.datePosted);
      const isDateFilterActive = cutoff !== null;
      const maxFetches = isDateFilterActive ? MAX_FILTER_PAGINATION_FETCHES : 1;

      let currentCursor = cursor ?? null;
      let fetchCount = 0;
      let lastResponse: JobsResponse | null = null;
      const aggregated: Job[] = [];

      while (fetchCount < maxFetches) {
        const paramsForFetch: Record<string, any> = { ...baseParams };
        if (currentCursor) paramsForFetch.cursor = currentCursor;

        const response = await fetchJobs(paramsForFetch);
        lastResponse = response;
        fetchCount += 1;

        const rawItems = response.items ?? [];
        const filteredItems = isDateFilterActive ? filterJobsByDate(rawItems, cutoff) : rawItems;
        aggregated.push(...filteredItems);

        currentCursor = response.nextCursor ?? null;

        const shouldContinue =
          isDateFilterActive &&
          aggregated.length < PAGE_SIZE &&
          Boolean(currentCursor) &&
          fetchCount < maxFetches;

        if (!shouldContinue) {
          break;
        }
      }

      const pageItems = aggregated.slice(0, PAGE_SIZE);
      let nextCursorValue: string | null = null;
      let hasMore = false;

      if (isDateFilterActive) {
        const moreFilteredAvailable = aggregated.length > PAGE_SIZE || Boolean(currentCursor);
        if (moreFilteredAvailable) {
          if (pageItems.length > 0) {
            const lastItem = pageItems[pageItems.length - 1];
            const encoded = encodeCursorValue(lastItem.postedAt, lastItem.id);
            if (encoded) {
              nextCursorValue = encoded;
              hasMore = true;
            } else if (currentCursor) {
              nextCursorValue = currentCursor;
              hasMore = true;
            }
          } else if (currentCursor) {
            nextCursorValue = currentCursor;
            hasMore = true;
          }
        }
      } else {
        nextCursorValue = lastResponse?.nextCursor ?? null;
        hasMore = Boolean(lastResponse?.nextCursor);
      }

      setJobs(prev => (reset ? pageItems : [...prev, ...pageItems]));
      setNextCursor(nextCursorValue);
      setHasMore(hasMore);
      // 自动选中第一个
      if (reset && pageItems.length > 0) {
        setSelectedJob((previous) => {
          if (isMobile && previous) {
            const stillExists = pageItems.some((item) => item.id === previous.id);
            if (stillExists) {
              return previous;
            }
          }
          return pageItems[0];
        });
        if (isMobile) {
          setIsMobileDetailOpen(false);
        }
      }
    } catch (e) {
      setHasMore(false);
    } finally {
      setLoading(false);
    }
  };

  // 首次和筛选/搜索时重置列表
  useEffect(() => {
    loadJobs(undefined, true);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [q, location, filters]);

  // 滚动加载 - 支持桌面和移动端
  const handleScroll = () => {
    const el = listRef.current;
    if (!el || loading || !hasMore || !nextCursor) return;
    const threshold = 100; // 增加阈值，提高移动端触发灵敏度
    const scrollPosition = el.scrollTop + el.clientHeight;
    const scrollHeight = el.scrollHeight;
    
    if (scrollHeight - scrollPosition < threshold) {
      // 只追加，不重置
      loadJobs(nextCursor, false);
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
        await loadJobs(undefined, true); // 重新加载列表
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
  const isInitialLoading = loading && jobs.length === 0;

  // 搜索/重置/筛选
  const handleReset = () => {
    setQ('');
    setLocation('');
    setFilters({ company: '', level: '', remote: '', salaryMin: '', datePosted: '' });
  };

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!hasPromptedSubscription.current) {
      setShowSubscriptionModal(true);
      setSubscriptionTrigger('search');
      hasPromptedSubscription.current = true;
    } else {
      loadJobs(undefined, true);
    }
  };

  const handleManualSubscription = () => {
    setShowSubscriptionModal(true);
    setSubscriptionTrigger('manual');
  };

  const handleConfirmSubscription = async () => {
    setShowSubscriptionModal(false);
    setSubscriptionTrigger(null);
    await fetch(`${API_BASE}/subscription`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(subscriptionParams),
    });
    loadJobs(undefined, true);
  };

  const handleCancelSubscription = () => {
    setShowSubscriptionModal(false);
    setSubscriptionTrigger(null);
    if (subscriptionTrigger === 'search') loadJobs(undefined, true);
  };

  const handleApplyFilters = () => {
    setShowFilterDrawer(false);
    loadJobs(undefined, true);
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
        onShowSubscription={handleManualSubscription}
        activeFilterCount={activeFilterCount}
        isSearching={loading}
      />

      <div className="grid gap-6 lg:grid-cols-[minmax(0,420px)_minmax(0,1fr)]">
        <Card className="border-white/60 bg-white/90 p-6 shadow-brand-lg backdrop-blur-sm lg:max-h-[70vh] lg:overflow-hidden relative">
          <div className="flex items-center justify-between gap-3">
            {/* 已移除列表结果数量显示 */}
          </div>
          
          {/* 下拉刷新指示器 */}
          {(pullDistance > 0 || isRefreshing) && (
            <div 
              className="absolute top-0 left-0 right-0 flex items-center justify-center bg-brand-50 border-b border-brand-100 transition-all duration-200"
              style={{ 
                height: `${Math.max(pullDistance, isRefreshing ? 60 : 0)}px`,
                transform: `translateY(-${Math.max(pullDistance, isRefreshing ? 60 : 0)}px)`
              }}
            >
              <div className="flex items-center gap-2 text-brand-600">
                {isRefreshing ? (
                  <>
                    <div className="animate-spin rounded-full h-4 w-4 border-2 border-brand-600 border-t-transparent"></div>
                    <span className="text-sm">正在刷新...</span>
                  </>
                ) : pullDistance > 50 ? (
                  <span className="text-sm">松开刷新</span>
                ) : (
                  <span className="text-sm">下拉刷新</span>
                )}
              </div>
            </div>
          )}
          
          <div
            className="mt-4 space-y-3 overflow-y-auto pr-1 lg:max-h-[52vh]"
            ref={listRef}
            onScroll={debouncedHandleScroll}
            onTouchStart={handleTouchStart}
            onTouchMove={handleTouchMove}
            onTouchEnd={handleTouchEnd}
            style={{ 
              WebkitOverflowScrolling: 'touch', // iOS 平滑滚动
              overscrollBehavior: 'contain', // 防止过度滚动
              transform: `translateY(${pullDistance}px)`, // 下拉时移动列表
              transition: pullDistance === 0 ? 'transform 0.2s ease-out' : 'none'
            }}
          >
            {isInitialLoading &&
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
            {jobs.map((job) => {
              const active = selectedJob?.id === job.id;
              return (
                <div
                  key={job.id}
                  className="cursor-pointer"
                  onClick={() => {
                    setSelectedJob(job);
                    if (isMobile) {
                      setIsMobileDetailOpen(true);
                    }
                  }}
                >
                  <JobCardNew
                    job={job}
                    className={active ? 'border-brand-500 shadow-brand-lg ring-2 ring-brand-200' : 'hover:border-brand-200/70'}
                  />
                </div>
              );
            })}
            {!isInitialLoading && jobs.length === 0 && (
              <div className="flex h-40 items-center justify-center rounded-3xl border border-dashed border-black/10 bg-white/70 text-sm text-gray-400">
                {t('search.results', { count: 0 })}
              </div>
            )}
            {/* 加载更多指示器和观察器元素 */}
            {loading && jobs.length > 0 && (
              <div className="flex justify-center mt-6">
                <Skeleton className="h-8 w-32" />
              </div>
            )}
            {!hasMore && jobs.length > 0 && (
              <div className="text-center text-gray-400 mt-4">没有更多数据了</div>
            )}
            {/* Intersection Observer 目标元素 */}
            <div ref={loadMoreRef} className="h-1 w-full" />
          </div>
        </Card>

        <Card className="hidden border-white/60 bg-white/95 p-6 shadow-brand-lg backdrop-blur-sm lg:block lg:max-h-[70vh] lg:overflow-y-auto">
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
        <div className="fixed inset-0 z-50 flex flex-col bg-white">
          <div className="flex items-center justify-between border-b border-black/10 px-4 py-3">
            <div className="min-w-0 flex-1 pr-3">
              <p className="truncate text-sm font-medium text-gray-600">{selectedJob.title}</p>
              <p className="truncate text-xs text-gray-400">
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

      <SubscriptionModal
        visible={showSubscriptionModal}
        onConfirm={handleConfirmSubscription}
        onCancel={handleCancelSubscription}
        params={subscriptionParams}
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

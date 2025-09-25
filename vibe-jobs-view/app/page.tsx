'use client';
import { useQuery } from '@tanstack/react-query';
import JobCard from '@/components/JobCard';
import type { JobsResponse } from '@/lib/types';
import { useMemo, useRef, useState } from 'react';

async function fetchJobs(params: Record<string, any>): Promise<JobsResponse> {
  const qs = new URLSearchParams(Object.entries(params).filter(([,v]) => v !== '' && v !== undefined) as any);
  const res = await fetch('/api/jobs?' + qs.toString(), { cache: 'no-store' });
  if (!res.ok) throw new Error('Failed to fetch jobs');
  return res.json();
}

function SubscriptionModal({ visible, onConfirm, onCancel, params }: { visible: boolean, onConfirm: () => void, onCancel: () => void, params: Record<string, any> }) {
  if (!visible) return null;
  return (
    <div className="fixed inset-0 bg-black bg-opacity-30 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-lg p-6 w-full max-w-md">
        <button className="absolute top-2 right-2 text-gray-400" onClick={onCancel}>×</button>
        <h2 className="text-xl font-bold mb-2">创建订阅提醒</h2>
        <p className="mb-4 text-gray-600">创建一个订阅，第一时间收到最新职位推送。你可以在个人中心管理订阅。</p>
        <div className="border rounded p-3 mb-4">
          <div className="font-semibold">搜索条件：</div>
          <div>关键词：{params.q || '不限'}</div>
          <div>公司：{params.company || '不限'}</div>
          <div>地点：{params.location || '不限'}</div>
          <div>级别：{params.level || '不限'}</div>
        </div>
        <div className="flex gap-3 justify-end">
          <button className="btn" onClick={onCancel}>取消</button>
          <button className="btn btn-primary" onClick={onConfirm}>确认订阅</button>
        </div>
      </div>
    </div>
  );
}

function FilterDrawer({ visible, onClose, filters, setFilters, onApply }: {
  visible: boolean,
  onClose: () => void,
  filters: any,
  setFilters: (f: any) => void,
  onApply: () => void
}) {
  if (!visible) return null;
  return (
    <div className="w-full max-w-xs shadow-lg p-6 relative bg-white rounded-lg">
      <button className="absolute top-4 right-4 text-gray-400" onClick={onClose}>×</button>
      <h2 className="text-lg font-bold mb-4">筛选职位</h2>
      <div className="space-y-4">
        <div>
          <label className="block text-sm font-medium mb-1">公司</label>
          <input className="input w-full" value={filters.company} onChange={e => setFilters({ ...filters, company: e.target.value })} placeholder="公司" />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">级别</label>
          <select className="select w-full" value={filters.level} onChange={e => setFilters({ ...filters, level: e.target.value })}>
            <option value="">不限</option>
            <option value="Junior">Junior</option>
            <option value="Mid">Mid</option>
            <option value="Senior">Senior</option>
            <option value="Staff">Staff</option>
            <option value="Principal">Principal</option>
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">远程</label>
          <select className="select w-full" value={filters.remote} onChange={e => setFilters({ ...filters, remote: e.target.value })}>
            <option value="">不限</option>
            <option value="true">远程</option>
            <option value="false">非远程</option>
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">薪资下限</label>
          <input className="input w-full" type="number" value={filters.salaryMin} onChange={e => setFilters({ ...filters, salaryMin: e.target.value })} placeholder="最低薪资" />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">发布日期</label>
          <select className="select w-full" value={filters.datePosted} onChange={e => setFilters({ ...filters, datePosted: e.target.value })}>
            <option value="">不限</option>
            <option value="1">1天内</option>
            <option value="3">3天内</option>
            <option value="7">7天内</option>
            <option value="30">30天内</option>
          </select>
        </div>
      </div>
      <div className="mt-6 flex gap-3 justify-end">
        <button className="btn" onClick={onClose}>取消</button>
        <button className="btn btn-primary" onClick={onApply}>应用</button>
      </div>
    </div>
  );
}

function JobDetail({ job }: { job: any }) {
  if (!job) return (
    <div className="flex items-center justify-center h-full text-gray-400 text-lg">请选择左侧职位查看详情</div>
  );
  return (
    <div className="p-6">
      <h2 className="text-2xl font-bold mb-2">{job.title}</h2>
      <div className="text-gray-600 mb-2">{job.company} · {job.location} {job.level ? `· ${job.level}` : ''}</div>
      <div className="mb-2 text-xs text-gray-500">{new Date(job.postedAt).toLocaleDateString()}</div>
      <div className="mb-4">
        <span className="font-semibold">标签：</span>
        {(job.tags ?? []).map((t: string) => <span key={t} className="badge border-gray-300 bg-gray-50 mr-1">{t}</span>)}
      </div>
      <div className="mb-4">
        <span className="font-semibold">描述：</span>
        <div className="mt-1 whitespace-pre-line text-sm">{job.description || '无详细描述'}</div>
      </div>
      {/* 可扩展更多详情字段 */}
    </div>
  );
}

function TopSearchBar({ q, setQ, location, setLocation, onSearch, onReset, onShowFilter, onShowSubscription, showFilterDrawer, setShowFilterDrawer, filters, setFilters }: {
  q: string;
  setQ: (v: string) => void;
  location: string;
  setLocation: (v: string) => void;
  onSearch: (e: React.FormEvent) => void;
  onReset: () => void;
  onShowFilter: () => void;
  onShowSubscription: () => void;
  showFilterDrawer: boolean;
  setShowFilterDrawer: (v: boolean) => void;
  filters: any;
  setFilters: (f: any) => void;
}) {
  return (
    <div className="sticky top-0 z-20 w-full bg-white shadow-sm border-b">
      <div className="max-w-7xl mx-auto flex items-center gap-4 px-6 py-4">
        <form className="flex flex-1 gap-3 items-center" onSubmit={onSearch}>
          <input className="input flex-1" placeholder="关键词 (如: backend, Java)" value={q} onChange={e => setQ(e.target.value)} />
          <input className="input flex-1" placeholder="地点" value={location} onChange={e => setLocation(e.target.value)} />
          <button className="btn" type="button" onClick={onReset}>重置</button>
          <button className="btn btn-primary" type="submit">搜索</button>
        </form>
        <div className="relative inline-block">
          <button className="btn btn-outline flex items-center gap-1" type="button" onClick={onShowFilter}>
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="4" y1="21" x2="4" y2="14"></line><line x1="4" y1="10" x2="4" y2="3"></line><line x1="12" y1="21" x2="12" y2="12"></line><line x1="12" y1="8" x2="15" y2="8"></line><line x1="20" y1="21" x2="20" y2="16"></line><line x1="20" y1="12" x2="20" y2="3"></line><line x1="1" y1="14" x2="7" y2="14"></line><line x1="9" y1="8" x2="15" y2="8"></line><line x1="17" y1="16" x2="23" y2="16"></line></svg>
            筛选
          </button>
          {showFilterDrawer && (
            <div style={{ position: 'absolute', top: '100%', left: 0, zIndex: 1000, width: 360, boxShadow: '0 4px 24px rgba(0,0,0,0.12)', background: '#fff', borderRadius: 8, marginTop: 8 }}>
              <FilterDrawer
                visible={showFilterDrawer}
                onClose={() => setShowFilterDrawer(false)}
                filters={filters}
                setFilters={setFilters}
                onApply={() => setShowFilterDrawer(false)}
              />
            </div>
          )}
        </div>
        <button className="flex items-center gap-2 btn btn-outline" type="button" onClick={onShowSubscription}>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="icon-bell" style={{display: 'inline', verticalAlign: 'middle'}}>
            <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9"></path>
            <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
          </svg>
          创建订阅提醒
        </button>
      </div>
    </div>
  );
}

export default function Page() {
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
  const [selectedJob, setSelectedJob] = useState<any>(null);

  const params = useMemo(() => ({ q, location, page, size, ...filters }), [q, location, page, size, filters]);
  const { data, isLoading, isError, error, refetch, isFetching } = useQuery({
    queryKey: ['jobs', params],
    queryFn: () => fetchJobs(params),
    keepPreviousData: true,
  });

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
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

  return (
    <div className="w-full h-full">
      <TopSearchBar
        q={q}
        setQ={setQ}
        location={location}
        setLocation={setLocation}
        onSearch={handleSearch}
        onReset={() => { setQ(''); setLocation(''); setPage(1); setFilters({ company: '', level: '', remote: '', salaryMin: '', datePosted: '' }); }}
        onShowFilter={() => setShowFilterDrawer(true)}
        onShowSubscription={handleManualSubscription}
        showFilterDrawer={showFilterDrawer}
        setShowFilterDrawer={setShowFilterDrawer}
        filters={filters}
        setFilters={setFilters}
      />
      <div className="flex gap-6 h-[calc(100vh-64px)] pt-2">
        {/* 左侧列表区 */}
        <div className="flex-1 min-w-[340px] max-w-[480px] overflow-y-auto border-r bg-white">
          <section className="space-y-3 p-4">
            <div className="flex items-center justify-between">
              <h2 className="text-sm text-gray-600">
                {isLoading ? 'Loading...' : `${data?.total ?? 0} results`}
                {isFetching && !isLoading ? ' · refreshing' : ''}
              </h2>
              <div className="text-xs text-gray-500">Page {data?.page ?? page}</div>
            </div>
            {isError && <div className="card p-4 text-red-600">Error: {(error as Error).message}</div>}
            <div className="grid gap-3">
              {(data?.items ?? []).map((job) => (
                <div
                  key={job.id}
                  className={`cursor-pointer rounded border transition ${selectedJob?.id === job.id ? 'border-blue-500 bg-blue-50' : 'border-transparent hover:border-gray-300'}`}
                  onClick={() => setSelectedJob(job)}
                >
                  <JobCard job={job} />
                </div>
              ))}
            </div>
            <div className="flex items-center justify-between pt-2">
              <button className="btn" disabled={(data?.page ?? 1) <= 1} onClick={() => setPage((p) => Math.max(1, p - 1))}>
                Previous
              </button>
              <button className="btn" disabled={((data?.page ?? 1) * (data?.size ?? size)) >= (data?.total ?? 0)} onClick={() => setPage((p) => p + 1)}>
                Next
              </button>
            </div>
          </section>
        </div>
        {/* 右侧详情区 */}
        <div className="flex-[2] min-w-[400px] max-w-[800px] h-full overflow-y-auto bg-white">
          <JobDetail job={selectedJob} />
        </div>
        {/* 弹窗和抽屉组件 */}
        <SubscriptionModal
          visible={showSubscriptionModal}
          onConfirm={handleConfirmSubscription}
          onCancel={handleCancelSubscription}
          params={params}
        />
        {/* FilterDrawer 已在 TopSearchBar 绝对定位弹窗中渲染，这里不再重复渲染 */}
      </div>
    </div>
  );
}

/** @jest-environment node */

import { renderToStaticMarkup } from 'react-dom/server';

import CrawlerBlueprintDetailPage from '@/app/(admin)/admin/crawler-blueprints/[code]/page';
import CrawlerBlueprintListPage from '@/app/(admin)/admin/crawler-blueprints/page';
import CrawlerBlueprintCreatePage from '@/app/(admin)/admin/crawler-blueprints/new/page';
import AdminLayout from '@/app/(admin)/admin/layout';
import {
  useCrawlerBlueprintDetail,
  useCrawlerBlueprints,
} from '@/modules/admin/hooks/useCrawlerBlueprints';
import type { CrawlerBlueprintDetail } from '@/modules/admin/types';
import type {
  useCrawlerBlueprintDetail as UseCrawlerBlueprintDetailFn,
  useCrawlerBlueprints as UseCrawlerBlueprintsFn,
} from '@/modules/admin/hooks/useCrawlerBlueprints';

jest.mock('next/link', () => ({
  __esModule: true,
  default: ({ children, href }: { children: any; href: string }) => <a href={href}>{children}</a>,
}));

jest.mock('next/navigation', () => ({
  usePathname: () => '/admin/crawler-blueprints',
  useRouter: () => ({ replace: jest.fn() }),
}));

jest.mock('@/modules/auth/hooks/useAuth', () => ({
  useAuth: () => ({
    user: { email: '975022570yp@gmail.com' },
    loading: false,
    logout: jest.fn(),
  }),
}));

jest.mock('@/modules/admin/hooks/useCrawlerBlueprints', () => ({
  __esModule: true,
  useCrawlerBlueprints: jest.fn(),
  useCrawlerBlueprintDetail: jest.fn(),
}));

type BlueprintsHookReturn = ReturnType<UseCrawlerBlueprintsFn>;

type DetailHookReturn = ReturnType<UseCrawlerBlueprintDetailFn>;

const mockUseCrawlerBlueprints = useCrawlerBlueprints as unknown as jest.MockedFunction<UseCrawlerBlueprintsFn>;
const mockUseCrawlerBlueprintDetail = useCrawlerBlueprintDetail as unknown as jest.MockedFunction<UseCrawlerBlueprintDetailFn>;

const baseDetail: CrawlerBlueprintDetail = {
  code: 'jd-tech',
  name: '京东技术岗位',
  enabled: true,
  description: '京东技术岗位 JD 爬虫',
  entryUrl: 'https://careers.jd.com/jobs',
  parserTemplateCode: 'jd-parser',
  concurrencyLimit: 4,
  lastRunStatus: 'SUCCESS',
  lastRunFinishedAt: '2024-05-01T12:00:00Z',
  createdAt: '2024-04-01T08:00:00Z',
  updatedAt: '2024-05-01T12:00:00Z',
  metrics: {
    totalRuns: 12,
    averageDurationMs: 180000,
    successRate: 0.92,
    lastRunDurationMs: 120000,
  },
  latestRuns: [
    {
      id: 'run-1',
      status: 'SUCCESS',
      startedAt: '2024-05-01T11:58:00Z',
      finishedAt: '2024-05-01T12:00:00Z',
      successCount: 120,
      failureCount: 3,
      message: 'Completed',
    },
  ],
  testReports: [
    {
      id: 'test-1',
      status: 'SUCCESS',
      createdAt: '2024-04-30T09:00:00Z',
      summary: 'All checkpoints passed',
      details: { snapshots: 4 },
    },
  ],
  activeTask: null,
  flow: [
    {
      step: '登录',
      description: '执行登录流程',
      status: 'SUCCESS',
    },
  ],
};

function setupBlueprintHook(overrides?: Partial<BlueprintsHookReturn>) {
  const list: BlueprintsHookReturn['list'] = {
    data: [baseDetail],
    isLoading: false,
    isError: false,
    error: null,
    refetch: jest.fn(),
    fetchStatus: 'idle',
    status: 'success',
  } as unknown as BlueprintsHookReturn['list'];

  const create: BlueprintsHookReturn['create'] = {
    mutate: jest.fn(),
    isPending: false,
  } as unknown as BlueprintsHookReturn['create'];

  const rerun: BlueprintsHookReturn['rerun'] = {
    mutate: jest.fn(),
    isPending: false,
  } as unknown as BlueprintsHookReturn['rerun'];

  const activate: BlueprintsHookReturn['activate'] = {
    mutate: jest.fn(),
    isPending: false,
  } as unknown as BlueprintsHookReturn['activate'];

  mockUseCrawlerBlueprints.mockReturnValue({
    list,
    create,
    rerun,
    activate,
    ...(overrides || {}),
  } as BlueprintsHookReturn);

  return { list, create, rerun, activate };
}

function setupDetailHook(detail?: CrawlerBlueprintDetail) {
  mockUseCrawlerBlueprintDetail.mockReturnValue({
    data: detail ?? baseDetail,
    isLoading: false,
    isError: false,
    error: null,
    refetch: jest.fn(),
    fetchStatus: 'idle',
    status: 'success',
  } as unknown as DetailHookReturn);
}

describe('Crawler blueprint admin pages', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders blueprint list with status badges', () => {
    setupBlueprintHook();

    const markup = renderToStaticMarkup(<CrawlerBlueprintListPage />);

    expect(markup).toContain('京东技术岗位');
    expect(markup).toContain('SUCCESS');
    expect(markup).toContain('/admin/crawler-blueprints/jd-tech');
  });

  it('renders blueprint detail including metrics and runs', () => {
    setupBlueprintHook();
    setupDetailHook();

    const markup = renderToStaticMarkup(
      <CrawlerBlueprintDetailPage params={{ code: 'jd-tech' }} />,
    );

    expect(markup).toContain('累计运行');
    expect(markup).toContain('run-1');
    expect(markup).toContain('测试报告');
  });

  it('renders creation wizard first step', () => {
    setupBlueprintHook();

    const markup = renderToStaticMarkup(<CrawlerBlueprintCreatePage />);

    expect(markup).toContain('新建爬虫蓝图');
    expect(markup).toContain('蓝图 Code');
    expect(markup).toContain('高阶选项');
    expect(markup).toContain('搜索关键词');
  });

  it('exposes crawler blueprint link in admin layout navigation', () => {
    setupBlueprintHook();

    const markup = renderToStaticMarkup(
      <AdminLayout>
        <div>child</div>
      </AdminLayout>,
    );

    expect(markup).toContain('/admin/crawler-blueprints');
  });
});

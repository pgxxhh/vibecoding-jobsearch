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
import type { CrawlerBlueprintDetail, CrawlerBlueprintSummary } from '@/modules/admin/types';
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

const baseSummary: CrawlerBlueprintSummary = {
  code: 'jd-tech',
  name: '京东技术岗位',
  enabled: true,
  entryUrl: 'https://careers.jd.com/jobs',
  status: 'SUCCESS',
  createdAt: '2024-04-01T08:00:00Z',
  updatedAt: '2024-05-01T12:00:00Z',
};

const baseDetail: CrawlerBlueprintDetail = {
  summary: baseSummary,
  draftConfig: null,
  lastTestReport: { status: 'SUCCESS', summary: 'All checkpoints passed' },
  recentTasks: [
    {
      id: 1,
      blueprintCode: 'jd-tech',
      status: 'SUCCEEDED',
      startedAt: '2024-05-01T11:58:00Z',
      finishedAt: '2024-05-01T12:00:00Z',
      errorMessage: null,
      browserSnapshot: null,
      sampleData: [],
      inputPayload: { entryUrl: 'https://careers.jd.com/jobs' },
    },
  ],
};

function setupBlueprintHook(overrides?: Partial<BlueprintsHookReturn>) {
    const list: BlueprintsHookReturn['list'] = {
      data: [baseSummary],
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

    expect(markup).toContain('最近生成任务');
    expect(markup).toContain('任务 #1');
    expect(markup).toContain('最近测试报告');
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

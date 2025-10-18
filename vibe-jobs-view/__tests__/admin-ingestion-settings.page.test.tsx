/** @jest-environment node */

import { renderToStaticMarkup } from 'react-dom/server';

import IngestionSettingsPage, {
  buildIngestionSettingsPayload,
  submitIngestionSettingsForm,
  type IngestionFormState,
} from '@/app/(admin)/admin/ingestion-settings/page';
import { useIngestionSettings } from '@/modules/admin/hooks/useIngestionSettings';
import type { IngestionSettings } from '@/modules/admin/types';

type IngestionHookReturn = ReturnType<typeof useIngestionSettings>;

jest.mock('@/modules/admin/hooks/useIngestionSettings');

const mockUseIngestionSettings = useIngestionSettings as jest.MockedFunction<typeof useIngestionSettings>;

const baseSettings: IngestionSettings = {
  fixedDelayMs: 3600000,
  initialDelayMs: 10000,
  pageSize: 100,
  recentDays: 7,
  concurrency: 4,
  companyOverrides: {},
  locationFilter: {},
  roleFilter: {},
  updatedAt: '2024-09-01T00:00:00.000Z',
};

function setupHookMock(overrides: Partial<IngestionHookReturn> = {}) {
  const query: IngestionHookReturn['query'] = {
    data: baseSettings,
    isLoading: false,
    isError: false,
    error: null,
    ...(overrides.query as object | undefined),
  } as unknown as IngestionHookReturn['query'];

  const update: IngestionHookReturn['update'] = {
    mutate: jest.fn(),
    isPending: false,
    ...(overrides.update as object | undefined),
  } as unknown as IngestionHookReturn['update'];

  mockUseIngestionSettings.mockReturnValue({
    query,
    update,
  });

  return { query, update };
}

describe('IngestionSettingsPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders loading state via hook', () => {
    setupHookMock({
      query: {
        data: undefined,
        isLoading: true,
      } as Partial<IngestionHookReturn['query']>,
    });

    const markup = renderToStaticMarkup(<IngestionSettingsPage />);

    expect(markup).toContain('加载配置中...');
  });

  it('renders error state via hook', () => {
    setupHookMock({
      query: {
        data: undefined,
        isError: true,
        error: new Error('失败了'),
      } as Partial<IngestionHookReturn['query']>,
    });

    const markup = renderToStaticMarkup(<IngestionSettingsPage />);

    expect(markup).toContain('失败了');
  });

  it('builds payload from form values and fallback data', () => {
    const form: IngestionFormState = {
      fixedDelayMs: '7200000',
      initialDelayMs: '15000',
      pageSize: '200',
      recentDays: '14',
      concurrency: '6',
      locationJson: '{"include":["北京"]}',
      roleJson: '{"keywords":["后端"]}',
    };

    const payload = buildIngestionSettingsPayload(form, baseSettings);

    expect(payload).toMatchObject({
      fixedDelayMs: 7200000,
      initialDelayMs: 15000,
      pageSize: 200,
      recentDays: 14,
      concurrency: 6,
      locationFilter: { include: ['北京'] },
      roleFilter: { keywords: ['后端'] },
    });
  });

  it('submits payload through mutate callback', () => {
    const mutate = jest.fn((variables: Partial<IngestionSettings>, options?: any) => {
      options?.onSuccess?.();
      return Promise.resolve(variables);
    });

    setupHookMock({
      update: {
        mutate,
      } as Partial<IngestionHookReturn['update']>,
    });

    const form: IngestionFormState = {
      fixedDelayMs: '7200000',
      initialDelayMs: '15000',
      pageSize: '200',
      recentDays: '14',
      concurrency: '6',
      locationJson: '{}',
      roleJson: '{}',
    };

    submitIngestionSettingsForm(form, {
      fallback: baseSettings,
      mutate,
      onSuccess: jest.fn(),
      onError: jest.fn(),
    });

    expect(mutate).toHaveBeenCalledWith(
      expect.objectContaining({
        fixedDelayMs: 7200000,
        recentDays: 14,
      }),
      expect.any(Object),
    );
  });

  it('throws when JSON cannot be parsed', () => {
    const form: IngestionFormState = {
      fixedDelayMs: '7200000',
      initialDelayMs: '15000',
      pageSize: '200',
      recentDays: '14',
      concurrency: '6',
      locationJson: '{',
      roleJson: '{}',
    };

    expect(() => buildIngestionSettingsPayload(form, baseSettings)).toThrow();
  });
});

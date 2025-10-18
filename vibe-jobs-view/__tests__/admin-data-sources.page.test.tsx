/** @jest-environment node */

import { renderToStaticMarkup } from 'react-dom/server';

import DataSourcesPage, {
  buildDataSourcePayload,
  removeDataSourceById,
  submitDataSourceForm,
  type DataSourceFormState,
} from '@/app/(admin)/admin/data-sources/page';
import { useDataSources } from '@/modules/admin/hooks/useDataSources';
import type { DataSourcePayload, DataSourceResponse } from '@/modules/admin/types';

type DataSourcesHookReturn = ReturnType<typeof useDataSources>;

jest.mock('@/modules/admin/components/DataSourceBulkUpload', () => () => null);
jest.mock('@/modules/admin/hooks/useDataSources');

const mockUseDataSources = useDataSources as jest.MockedFunction<typeof useDataSources>;

const baseDataSource: DataSourceResponse = {
  id: 1,
  code: 'source-1',
  type: 'lever',
  enabled: true,
  runOnStartup: true,
  requireOverride: false,
  flow: 'UNLIMITED',
  baseOptions: { region: 'cn' },
  categories: [],
  companies: [],
};

function setupDataSourcesMock(overrides: Partial<DataSourcesHookReturn> = {}) {
  const list: DataSourcesHookReturn['list'] = {
    data: [baseDataSource],
    isLoading: false,
    isError: false,
    error: null,
    ...(overrides.list as object | undefined),
  } as unknown as DataSourcesHookReturn['list'];

  const save: DataSourcesHookReturn['save'] = {
    mutate: jest.fn(),
    isPending: false,
    ...(overrides.save as object | undefined),
  } as unknown as DataSourcesHookReturn['save'];

  const remove: DataSourcesHookReturn['remove'] = {
    mutate: jest.fn(),
    isPending: false,
    ...(overrides.remove as object | undefined),
  } as unknown as DataSourcesHookReturn['remove'];

  const bulkUpload: DataSourcesHookReturn['bulkUpload'] = {
    mutate: jest.fn(),
    isPending: false,
    ...(overrides.bulkUpload as object | undefined),
  } as unknown as DataSourcesHookReturn['bulkUpload'];

  mockUseDataSources.mockReturnValue({
    list,
    save,
    remove,
    bulkUpload,
  } as DataSourcesHookReturn);

  return { list, save, remove, bulkUpload };
}

describe('DataSourcesPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders loading state via hook', () => {
    setupDataSourcesMock({
      list: {
        data: undefined,
        isLoading: true,
      } as Partial<DataSourcesHookReturn['list']>,
    });

    const markup = renderToStaticMarkup(<DataSourcesPage />);

    expect(markup).toContain('加载中...');
  });

  it('renders error state via hook', () => {
    setupDataSourcesMock({
      list: {
        data: undefined,
        isError: true,
        error: new Error('出错了'),
      } as Partial<DataSourcesHookReturn['list']>,
    });

    const markup = renderToStaticMarkup(<DataSourcesPage />);

    expect(markup).toContain('出错了');
  });

  it('builds payload from form JSON values', () => {
    const form: DataSourceFormState = {
      code: 'new-source',
      type: 'greenhouse',
      enabled: true,
      runOnStartup: false,
      requireOverride: false,
      flow: 'UNLIMITED',
      baseOptionsJson: '{"key":"value"}',
      categoriesJson: '[{"name":"dev","limit":10,"tags":[],"facets":{}}]',
      companiesJson: '[]',
    };

    const payload = buildDataSourcePayload(form);

    expect(payload).toMatchObject({
      code: 'new-source',
      type: 'greenhouse',
      baseOptions: { key: 'value' },
      categories: [{ name: 'dev', limit: 10 }],
      companies: [],
    });
  });

  it('submits payload using save mutation', () => {
    const mutate = jest.fn(
      (args: { payload: DataSourcePayload; id?: number | 'new' | null }, options?: any) => {
        options?.onSuccess?.();
        return Promise.resolve(args);
      },
    );

    setupDataSourcesMock({
      save: {
        mutate,
      } as Partial<DataSourcesHookReturn['save']>,
      list: {
        data: [],
      } as Partial<DataSourcesHookReturn['list']>,
    });

    const form: DataSourceFormState = {
      code: 'new-source',
      type: 'greenhouse',
      enabled: true,
      runOnStartup: false,
      requireOverride: false,
      flow: 'UNLIMITED',
      baseOptionsJson: '{}',
      categoriesJson: '[]',
      companiesJson: '[]',
    };

    submitDataSourceForm(form, {
      id: 'new',
      mutate,
      onSuccess: jest.fn(),
      onError: jest.fn(),
    });

    expect(mutate).toHaveBeenCalledWith(
      expect.objectContaining({
        payload: expect.objectContaining({ code: 'new-source' }),
        id: 'new',
      }),
      expect.any(Object),
    );
  });

  it('removes data source via helper', () => {
    const mutate = jest.fn((id: number, options?: any) => {
      options?.onSuccess?.();
      return Promise.resolve(id);
    });

    setupDataSourcesMock({
      remove: {
        mutate,
      } as Partial<DataSourcesHookReturn['remove']>,
    });

    removeDataSourceById(1, {
      mutate,
      onSuccess: jest.fn(),
      onError: jest.fn(),
    });

    expect(mutate).toHaveBeenCalledWith(1, expect.any(Object));
  });

  it('throws when JSON parsing fails', () => {
    const form: DataSourceFormState = {
      code: 'invalid',
      type: 'lever',
      enabled: true,
      runOnStartup: false,
      requireOverride: false,
      flow: 'UNLIMITED',
      baseOptionsJson: '{',
      categoriesJson: '[]',
      companiesJson: '[]',
    };

    expect(() => buildDataSourcePayload(form)).toThrow();
  });
});

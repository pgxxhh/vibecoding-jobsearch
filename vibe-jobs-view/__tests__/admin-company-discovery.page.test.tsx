/** @jest-environment node */

import { renderToStaticMarkup } from 'react-dom/server';

import CompanyDiscoveryPage from '@/app/(admin)/admin/company-discovery/page';
import { useCompanyDiscoverySettings } from '@/modules/admin/hooks/useCompanyDiscoverySettings';
import { useCompanyDiscoveryRuns } from '@/modules/admin/hooks/useCompanyDiscoveryRuns';
import { useCompanyDiscoveryResults } from '@/modules/admin/hooks/useCompanyDiscoveryResults';
import type { CompanyDiscoverySettings } from '@/modules/admin/types';
import {
  buildCompanyDiscoverySettingsPayload,
  submitCompanyDiscoverySettingsForm,
  type CompanyDiscoveryFormState,
} from '@/modules/admin/utils/companyDiscoveryForm';

jest.mock('@/modules/admin/hooks/useCompanyDiscoverySettings');
jest.mock('@/modules/admin/hooks/useCompanyDiscoveryRuns');
jest.mock('@/modules/admin/hooks/useCompanyDiscoveryResults');

const mockUseCompanyDiscoverySettings = useCompanyDiscoverySettings as jest.MockedFunction<typeof useCompanyDiscoverySettings>;
const mockUseCompanyDiscoveryRuns = useCompanyDiscoveryRuns as jest.MockedFunction<typeof useCompanyDiscoveryRuns>;
const mockUseCompanyDiscoveryResults = useCompanyDiscoveryResults as jest.MockedFunction<typeof useCompanyDiscoveryResults>;

const baseSettings: CompanyDiscoverySettings = {
  enabled: true,
  fixedDelayMs: 86400000,
  initialDelayMs: 10000,
  pageSize: 50,
  maxCandidatesPerRun: 200,
  dryRun: true,
  includeDataSourceTypes: ['smartrecruiters'],
  excludeCompanies: [],
  providers: {},
  locationFilter: {},
  roleFilter: {},
  updatedAt: '2024-09-01T00:00:00.000Z',
};

function setupHookMock() {
  mockUseCompanyDiscoverySettings.mockReturnValue({
    query: {
      data: baseSettings,
      isLoading: false,
      isError: false,
      error: null,
    } as any,
    update: {
      mutate: jest.fn(),
      isPending: false,
    } as any,
  });

  mockUseCompanyDiscoveryRuns.mockReturnValue({
    query: {
      data: [],
      isLoading: false,
    } as any,
    trigger: {
      mutate: jest.fn(),
      isPending: false,
    } as any,
  });

  mockUseCompanyDiscoveryResults.mockReturnValue({
    data: [],
    isLoading: false,
  } as any);
}

describe('CompanyDiscoveryPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders loading state when settings are loading', () => {
    mockUseCompanyDiscoverySettings.mockReturnValue({
      query: {
        data: undefined,
        isLoading: true,
        isError: false,
        error: null,
      } as any,
      update: {
        mutate: jest.fn(),
        isPending: false,
      } as any,
    });
    mockUseCompanyDiscoveryRuns.mockReturnValue({
      query: { data: [], isLoading: false } as any,
      trigger: { mutate: jest.fn(), isPending: false } as any,
    });
    mockUseCompanyDiscoveryResults.mockReturnValue({ data: [], isLoading: false } as any);

    const markup = renderToStaticMarkup(<CompanyDiscoveryPage />);

    expect(markup).toContain('加载配置中...');
  });

  it('builds payload from form values', () => {
    const form: CompanyDiscoveryFormState = {
      enabled: true,
      fixedDelayMs: '120000',
      initialDelayMs: '5000',
      pageSize: '30',
      maxCandidatesPerRun: '150',
      dryRun: false,
      includeDataSourceTypes: 'smartrecruiters, lever',
      excludeCompanies: 'bad-co',
      providersJson: '{"seed":{"enabled":true}}',
      locationJson: '{}',
      roleJson: '{}',
    };

    const payload = buildCompanyDiscoverySettingsPayload(form, baseSettings);

    expect(payload).toMatchObject({
      enabled: true,
      fixedDelayMs: 120000,
      pageSize: 30,
      maxCandidatesPerRun: 150,
      includeDataSourceTypes: ['smartrecruiters', 'lever'],
      excludeCompanies: ['bad-co'],
    });
  });

  it('submits payload through mutate callback', () => {
    const mutate = jest.fn((variables: Partial<CompanyDiscoverySettings>, options?: any) => {
      options?.onSuccess?.();
      return Promise.resolve(variables);
    });

    setupHookMock();

    const form: CompanyDiscoveryFormState = {
      enabled: true,
      fixedDelayMs: '120000',
      initialDelayMs: '5000',
      pageSize: '30',
      maxCandidatesPerRun: '150',
      dryRun: false,
      includeDataSourceTypes: '',
      excludeCompanies: '',
      providersJson: '{}',
      locationJson: '{}',
      roleJson: '{}',
    };

    submitCompanyDiscoverySettingsForm(form, {
      fallback: baseSettings,
      mutate,
      onSuccess: jest.fn(),
      onError: jest.fn(),
    });

    expect(mutate).toHaveBeenCalledWith(
      expect.objectContaining({
        enabled: true,
        fixedDelayMs: 120000,
      }),
      expect.any(Object),
    );
  });
});

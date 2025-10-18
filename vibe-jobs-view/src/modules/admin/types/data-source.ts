export interface CategoryQuotaDefinition {
  name: string;
  limit: number;
  tags: string[];
  facets: Record<string, string[]>;
}

export interface DataSourceCompany {
  id: number | null;
  reference: string;
  displayName: string;
  slug: string;
  enabled: boolean;
  placeholderOverrides: Record<string, string>;
  overrideOptions: Record<string, string>;
}

export interface DataSourceResponse {
  id: number;
  code: string;
  type: string;
  enabled: boolean;
  runOnStartup: boolean;
  requireOverride: boolean;
  flow: 'LIMITED' | 'UNLIMITED';
  baseOptions: Record<string, string>;
  categories: CategoryQuotaDefinition[];
  companies: DataSourceCompany[];
}

export interface DataSourcePayload {
  code: string;
  type: string;
  enabled: boolean;
  runOnStartup: boolean;
  requireOverride: boolean;
  flow: 'LIMITED' | 'UNLIMITED';
  baseOptions: Record<string, unknown>;
  categories: CategoryQuotaDefinition[];
  companies: DataSourceCompany[];
}

export interface PagedCompanyResponse {
  content: DataSourceCompany[];
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface PagedDataSourceResponse {
  id: number;
  code: string;
  type: string;
  enabled: boolean;
  runOnStartup: boolean;
  requireOverride: boolean;
  flow: 'LIMITED' | 'UNLIMITED';
  baseOptions: Record<string, string>;
  companies: PagedCompanyResponse;
}

export interface CompanyBulkPayload {
  reference: string;
  displayName?: string;
  slug?: string;
  enabled?: boolean;
  placeholderOverrides?: Record<string, string>;
  overrideOptions?: Record<string, string>;
}

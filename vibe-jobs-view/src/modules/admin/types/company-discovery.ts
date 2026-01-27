export interface CompanyDiscoveryProviderSettings {
  enabled: boolean;
  baseUrl?: string;
  seedCompanies?: string[];
}

export interface CompanyDiscoverySettings {
  enabled: boolean;
  fixedDelayMs: number;
  initialDelayMs: number;
  pageSize: number;
  maxCandidatesPerRun: number;
  dryRun: boolean;
  includeDataSourceTypes: string[];
  excludeCompanies: string[];
  providers: Record<string, CompanyDiscoveryProviderSettings>;
  locationFilter: Record<string, unknown>;
  roleFilter: Record<string, unknown>;
  updatedAt: string;
}

export interface CompanyDiscoveryRun {
  id: number | null;
  status: string;
  provider: string;
  dryRun: boolean;
  totalCandidates: number;
  totalValid: number;
  startedAt: string | null;
  completedAt: string | null;
}

export interface CompanyDiscoveryResult {
  id: number;
  runId: number;
  dataSourceCode: string;
  companyReference: string;
  displayName?: string;
  provider?: string;
  status: string;
  reason?: string;
  createdAt: string;
}

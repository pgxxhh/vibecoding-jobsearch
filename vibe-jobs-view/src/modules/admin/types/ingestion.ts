export interface IngestionSettings {
  fixedDelayMs: number;
  initialDelayMs: number;
  pageSize: number;
  recentDays: number;
  concurrency: number;
  companyOverrides: Record<string, unknown>;
  locationFilter: unknown;
  roleFilter: unknown;
  updatedAt: string;
}

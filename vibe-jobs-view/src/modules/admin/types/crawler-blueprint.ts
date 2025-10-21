export type CrawlerBlueprintStatus =
  | 'IDLE'
  | 'RUNNING'
  | 'SUCCESS'
  | 'FAILED'
  | 'PENDING'
  | string;

export interface CrawlerBlueprintSummary {
  code: string;
  name: string;
  enabled: boolean;
  entryUrl?: string | null;
  concurrencyLimit?: number | null;
  parserTemplateCode?: string | null;
  lastRunStatus?: CrawlerBlueprintStatus | null;
  lastRunFinishedAt?: string | null;
  activeTaskId?: string | null;
  description?: string | null;
}

export interface CrawlerBlueprintRun {
  id: string;
  status: CrawlerBlueprintStatus;
  startedAt?: string | null;
  finishedAt?: string | null;
  totalItems?: number | null;
  successCount?: number | null;
  failureCount?: number | null;
  message?: string | null;
}

export interface CrawlerBlueprintTestReport {
  id: string;
  createdAt?: string | null;
  status: CrawlerBlueprintStatus;
  summary?: string | null;
  details?: string | Record<string, unknown> | null;
}

export interface CrawlerBlueprintDetail extends CrawlerBlueprintSummary {
  createdAt?: string | null;
  updatedAt?: string | null;
  automation?: Record<string, unknown> | null;
  flow?: Array<{
    step: string;
    description?: string | null;
    status?: CrawlerBlueprintStatus | null;
  }>;
  metrics?: {
    totalRuns?: number | null;
    averageDurationMs?: number | null;
    successRate?: number | null;
    lastRunDurationMs?: number | null;
  } | null;
  latestRuns?: CrawlerBlueprintRun[];
  testReports?: CrawlerBlueprintTestReport[];
  activeTask?: {
    id: string;
    status: CrawlerBlueprintStatus;
    startedAt?: string | null;
    kind?: string | null;
  } | null;
}

export interface CreateCrawlerBlueprintPayload {
  code?: string;
  name?: string;
  entryUrl: string;
  searchKeywords?: string;
  excludeSelectors?: string[];
  notes?: string;
}

export interface ActivateCrawlerBlueprintPayload {
  code: string;
  enabled: boolean;
}

export interface RerunCrawlerBlueprintPayload {
  code: string;
  name?: string;
  entryUrl?: string;
  searchKeywords?: string;
  excludeSelectors?: string[];
  notes?: string;
}

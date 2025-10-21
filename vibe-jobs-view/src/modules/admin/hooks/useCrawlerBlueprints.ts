import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  activateCrawlerBlueprint,
  createCrawlerBlueprint,
  fetchCrawlerBlueprintDetail,
  fetchCrawlerBlueprints,
  rerunCrawlerBlueprint,
} from '@/modules/admin/services/crawlerBlueprintService';
import type {
  ActivateCrawlerBlueprintPayload,
  CreateCrawlerBlueprintPayload,
  CrawlerBlueprintDetail,
  CrawlerBlueprintSummary,
  RerunCrawlerBlueprintPayload,
} from '@/modules/admin/types';

export function useCrawlerBlueprints() {
  const queryClient = useQueryClient();

  const list = useQuery<CrawlerBlueprintSummary[]>({
    queryKey: ['admin', 'crawler-blueprints'],
    queryFn: fetchCrawlerBlueprints,
    refetchInterval: 15_000,
  });

  const create = useMutation({
    mutationFn: (payload: CreateCrawlerBlueprintPayload) => createCrawlerBlueprint(payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin', 'crawler-blueprints'] });
    },
  });

  const rerun = useMutation({
    mutationFn: (payload: RerunCrawlerBlueprintPayload) => rerunCrawlerBlueprint(payload),
    onSuccess: async (_result, variables) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['admin', 'crawler-blueprints'] }),
        queryClient.invalidateQueries({ queryKey: ['admin', 'crawler-blueprints', variables.code] }),
      ]);
    },
  });

  const activate = useMutation({
    mutationFn: (payload: ActivateCrawlerBlueprintPayload) => activateCrawlerBlueprint(payload),
    onSuccess: async (_result, variables) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['admin', 'crawler-blueprints'] }),
        queryClient.invalidateQueries({ queryKey: ['admin', 'crawler-blueprints', variables.code] }),
      ]);
    },
  });

  return {
    list,
    create,
    rerun,
    activate,
  };
}

export function useCrawlerBlueprintDetail(code: string | null) {
  return useQuery<CrawlerBlueprintDetail>({
    queryKey: ['admin', 'crawler-blueprints', code],
    queryFn: () => fetchCrawlerBlueprintDetail(code as string),
    enabled: Boolean(code),
    refetchInterval: (data) => {
      if (!data?.activeTask) {
        return false;
      }
      const status = data.activeTask.status?.toUpperCase?.() ?? data.activeTask.status;
      return status === 'RUNNING' || status === 'PENDING' ? 5_000 : false;
    },
  });
}

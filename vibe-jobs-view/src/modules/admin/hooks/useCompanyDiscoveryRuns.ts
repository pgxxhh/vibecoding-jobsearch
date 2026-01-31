import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  fetchCompanyDiscoveryRuns,
  triggerCompanyDiscoveryRun,
} from '@/modules/admin/services/companyDiscoveryService';
import type { CompanyDiscoveryRun } from '@/modules/admin/types';

export function useCompanyDiscoveryRuns() {
  const queryClient = useQueryClient();
  const query = useQuery<CompanyDiscoveryRun[]>({
    queryKey: ['admin', 'company-discovery', 'runs'],
    queryFn: fetchCompanyDiscoveryRuns,
  });

  const trigger = useMutation({
    mutationFn: triggerCompanyDiscoveryRun,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin', 'company-discovery', 'runs'] });
      await queryClient.invalidateQueries({ queryKey: ['admin', 'company-discovery', 'results'] });
    },
  });

  return { query, trigger };
}

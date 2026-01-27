import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  fetchCompanyDiscoverySettings,
  updateCompanyDiscoverySettings,
} from '@/modules/admin/services/companyDiscoveryService';
import type { CompanyDiscoverySettings } from '@/modules/admin/types';

export function useCompanyDiscoverySettings() {
  const queryClient = useQueryClient();
  const query = useQuery<CompanyDiscoverySettings>({
    queryKey: ['admin', 'company-discovery', 'settings'],
    queryFn: fetchCompanyDiscoverySettings,
  });

  const update = useMutation({
    mutationFn: updateCompanyDiscoverySettings,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin', 'company-discovery', 'settings'] });
    },
  });

  return { query, update };
}

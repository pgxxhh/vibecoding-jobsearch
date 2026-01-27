import { useQuery } from '@tanstack/react-query';
import { fetchCompanyDiscoveryResults } from '@/modules/admin/services/companyDiscoveryService';
import type { CompanyDiscoveryResult } from '@/modules/admin/types';

export function useCompanyDiscoveryResults() {
  return useQuery<CompanyDiscoveryResult[]>({
    queryKey: ['admin', 'company-discovery', 'results'],
    queryFn: fetchCompanyDiscoveryResults,
  });
}

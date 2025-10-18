import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  bulkUploadCompanies,
  deleteCompany,
  fetchDataSourcePaged,
  saveCompany,
} from '@/modules/admin/services/dataSourcesService';
import type { CompanyBulkPayload, PagedDataSourceResponse } from '@/modules/admin/types';

export function useDataSourceCompanies(codeOrId: string, page: number, size: number) {
  const queryClient = useQueryClient();
  const query = useQuery<PagedDataSourceResponse>({
    queryKey: ['admin', 'data-source-paged', codeOrId, page, size],
    queryFn: () => fetchDataSourcePaged(codeOrId, page, size),
  });

  const save = useMutation({
    mutationFn: (company: Partial<CompanyBulkPayload> & { id?: number | null }) => {
      const dataSourceCode = query.data?.code ?? codeOrId;
      return saveCompany(dataSourceCode, company);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin', 'data-source-paged', codeOrId] });
    },
  });

  const remove = useMutation({
    mutationFn: (companyId: number) => {
      const dataSourceCode = query.data?.code ?? codeOrId;
      return deleteCompany(dataSourceCode, companyId);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin', 'data-source-paged', codeOrId] });
    },
  });

  const bulkUpload = useMutation({
    mutationFn: (companies: CompanyBulkPayload[]) => {
      const dataSourceCode = query.data?.code ?? codeOrId;
      return bulkUploadCompanies(dataSourceCode, companies);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin', 'data-source-paged', codeOrId] });
    },
  });

  return {
    query,
    save,
    remove,
    bulkUpload,
  };
}

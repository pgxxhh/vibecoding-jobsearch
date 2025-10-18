import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  bulkUploadDataSources,
  deleteDataSource,
  fetchDataSources,
  saveDataSource,
} from '@/modules/admin/services/dataSourcesService';
import type { DataSourcePayload, DataSourceResponse } from '@/modules/admin/types';

export function useDataSources() {
  const queryClient = useQueryClient();
  const list = useQuery<DataSourceResponse[]>({
    queryKey: ['admin', 'data-sources'],
    queryFn: fetchDataSources,
  });

  const save = useMutation({
    mutationFn: ({ payload, id }: { payload: DataSourcePayload; id?: number | 'new' | null }) =>
      saveDataSource(payload, id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin', 'data-sources'] });
    },
  });

  const remove = useMutation({
    mutationFn: (id: number) => deleteDataSource(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin', 'data-sources'] });
    },
  });

  const bulkUpload = useMutation({
    mutationFn: bulkUploadDataSources,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin', 'data-sources'] });
    },
  });

  return {
    list,
    save,
    remove,
    bulkUpload,
  };
}
